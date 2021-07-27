/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.overridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.getExposingGetter
import org.jetbrains.kotlin.fir.resolve.hasExposingGetter
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.upperBoundIfFlexible
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext

object FirOverrideChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val typeCheckerContext = context.session.typeContext.newBaseTypeCheckerContext(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )

        val firTypeScope = declaration.unsubstitutedScope(context)

        for (it in declaration.declarations) {
            if (it is FirSimpleFunction || it is FirProperty) {
                checkMember((it as FirCallableDeclaration).symbol, declaration, reporter, typeCheckerContext, firTypeScope, context)
            }
        }
    }

    private fun FirTypeScope.retrieveDirectOverriddenOf(memberSymbol: FirCallableSymbol<*>): List<FirCallableSymbol<*>> {
        return when (memberSymbol) {
            is FirNamedFunctionSymbol -> {
                processFunctionsByName(memberSymbol.name) {}
                getDirectOverriddenFunctions(memberSymbol)
            }
            is FirPropertySymbol -> {
                processPropertiesByName(memberSymbol.name) {}
                getDirectOverriddenProperties(memberSymbol)
            }
            else -> throw IllegalArgumentException("unexpected member kind $memberSymbol")
        }
    }

    private fun ConeKotlinType.substituteAllTypeParameters(
        overrideDeclaration: FirCallableSymbol<*>,
        baseDeclaration: FirCallableSymbol<*>,
        context: CheckerContext
    ): ConeKotlinType {
        val overrideTypeParameters = overrideDeclaration.typeParameterSymbols
        if (overrideTypeParameters.isEmpty()) {
            return this
        }

        val baseTypeParameters = baseDeclaration.typeParameterSymbols

        val map = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
        val size = minOf(overrideTypeParameters.size, baseTypeParameters.size)

        for (it in 0 until size) {
            val to = overrideTypeParameters[it]
            val from = baseTypeParameters[it]

            map[from] = to.toConeType()
        }

        return substitutorByMap(map, context.session).substituteOrSelf(this)
    }

    private fun checkModality(
        overriddenSymbols: List<FirCallableSymbol<*>>,
    ): FirCallableSymbol<*>? {
        for (overridden in overriddenSymbols) {
            val modality = overridden.modality
            val isEffectivelyFinal = modality == null || modality == Modality.FINAL
            if (isEffectivelyFinal) {
                return overridden
            }
        }
        return null
    }

    private fun FirPropertySymbol.checkMutability(
        overriddenSymbols: List<FirCallableSymbol<*>>,
    ): FirCallableSymbol<*>? {
        if (isVar) return null
        return overriddenSymbols.find { (it as? FirPropertySymbol)?.isVar == true }
    }

    private inline fun Boolean.onTrue(callback: () -> Unit) {
        if (this) {
            callback()
        }
    }

    /**
     * Simplifies passing around visibility
     * data.
     */
    private data class VisibilityInfo(
        val symbol: FirCallableSymbol<*>,
        /**
         * There is no guarantee whether this visibility
         * is the symbol.visibility or a property getter's one.
         */
        val visibility: Visibility,
    )

    /**
     * Prepares visibility information for
     * further use.
     */
    private class VisibilityInfoProvider(val symbol: FirCallableSymbol<*>) {
        val exposingGetter = symbol.getExposingGetter()

        val hasExposingGetter: Boolean
            get() = exposingGetter != null

        val actualVisibility: Visibility
            get() = exposingGetter?.visibility ?: symbol.visibility

        val actualInfo: VisibilityInfo
            get() = VisibilityInfo(symbol, actualVisibility)

        val formalInfo: VisibilityInfo
            get() = VisibilityInfo(symbol, symbol.visibility)
    }

    /**
     * Returns true if some diagnostic has been
     * reported.
     */
    private fun VisibilityInfo.checkAccessPrivilegeOrOnWeaker(
        other: VisibilityInfo,
        reporter: DiagnosticReporter,
        context: CheckerContext,
        onWeaker: () -> Unit
    ): Boolean {
        val compare = Visibilities.compare(visibility, other.visibility)

        if (compare == null) {
            reporter.reportCannotChangeAccessPrivilege(symbol, other.symbol, context)
            return true
        } else if (compare < 0) {
            onWeaker()
            return true
        }

        return false
    }

    /**
     * Returns true if some diagnostic has been
     * reported.
     */
    private fun VisibilityInfoProvider.compareWith(
        other: VisibilityInfoProvider,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ): Boolean {
        actualInfo.checkAccessPrivilegeOrOnWeaker(other.actualInfo, reporter, context) {
            if (!hasExposingGetter && other.hasExposingGetter) {
                reporter.reportIncompletePropertyOverride(other.actualVisibility, symbol, context)
            } else {
                reporter.reportCannotWeakenAccessPrivilege(symbol, other.symbol, context)
            }
        }.onTrue {
            return true
        }

        // This particular case means we are
        // overriding a property with an exposing
        // getter with a new property with some new
        // exposing getter. Previous check resulted
        // in comparing the visibilities of the getters
        // and now we'll check the visibilities of the
        // properties themselves to make sure that
        // the overridden one has a greater visibility.

        if (hasExposingGetter && other.hasExposingGetter) {
            formalInfo.checkAccessPrivilegeOrOnWeaker(other.formalInfo, reporter, context) {
                reporter.reportCannotWeakenAccessPrivilege(symbol, other.symbol, context)
            }.onTrue {
                return true
            }
        }

        return false
    }

    private fun FirCallableSymbol<*>.checkVisibility(
        containingClass: FirClass,
        reporter: DiagnosticReporter,
        overriddenSymbols: List<FirCallableSymbol<*>>,
        context: CheckerContext
    ) {
        val selfInfo = VisibilityInfoProvider(this)

        val visibilities = overriddenSymbols.map {
            VisibilityInfoProvider(it)
        }.sortedBy {
            // Regard `null` compare as Int.MIN so that we can report CANNOT_CHANGE_... first deterministically
            Visibilities.compare(selfInfo.actualVisibility, it.actualVisibility) ?: Int.MIN_VALUE
        }

        for (overriddenInfo in visibilities) {
            selfInfo.compareWith(
                overriddenInfo,
                reporter,
                context
            ).onTrue {
                return
            }
        }

        val file = context.findClosest<FirFile>() ?: return
        val containingDeclarations = context.containingDeclarations + containingClass
        val visibilityChecker = context.session.visibilityChecker
        val hasVisibleBase = overriddenSymbols.any {
            it.ensureResolved(FirResolvePhase.STATUS)
            @OptIn(SymbolInternals::class)
            val fir = it.fir
            val firExposingGetter = fir.getExposingGetter()
            if (firExposingGetter != null) {
                visibilityChecker.isVisible(firExposingGetter, context.session, file, containingDeclarations, null)
            } else {
                visibilityChecker.isVisible(fir, context.session, file, containingDeclarations, null)
            }
        }
        if (!hasVisibleBase) {
            //NB: Old FE reports this in an attempt to override private member,
            //while the new FE doesn't treat super's private members as overridable, so you won't get them here
            //(except for properties with exposing getters)
            //instead you will get NOTHING_TO_OVERRIDE, which seems acceptable
            reporter.reportOn(source, FirErrors.CANNOT_OVERRIDE_INVISIBLE_MEMBER, this, overriddenSymbols.first(), context)
        }
    }

    // See [OverrideResolver#isReturnTypeOkForOverride]
    private fun FirCallableSymbol<*>.checkReturnType(
        overriddenSymbols: List<FirCallableSymbol<*>>,
        typeCheckerContext: AbstractTypeCheckerContext,
        context: CheckerContext,
    ): FirCallableSymbol<*>? {
        val overridingReturnType = resolvedReturnTypeRef.coneType

        // Don't report *_ON_OVERRIDE diagnostics according to an error return type. That should be reported separately.
        if (overridingReturnType is ConeKotlinErrorType) {
            return null
        }

        val isVar = this is FirPropertySymbol && this.isVar
        val bounds = if (isVar || this.hasExposingGetter() || visibility != Visibilities.Private) {
            // We should check the property's own type against parent
            // properties' own types, and their exposing getters will
            // be checked by `checkPermissiveGetter()`.
            // For a var we must require the properties' own types
            // match exactly to support setters.
            overriddenSymbols.map { context.returnTypeCalculator.tryCalculateReturnType(it).coneType.upperBoundIfFlexible() }
        } else {
            // We are working with a usual property, so its
            // type must be consistent either with the parent property's
            // own type or its exposing getter if present.
            overriddenSymbols.map {
                val typeHolder = it.getExposingGetter() ?: it
                context.returnTypeCalculator.tryCalculateReturnType(typeHolder).coneType.upperBoundIfFlexible()
            }
        }

        for (it in bounds.indices) {
            val overriddenDeclaration = overriddenSymbols[it]

            val overriddenReturnType = bounds[it].substituteAllTypeParameters(this, overriddenDeclaration, context)

            val isReturnTypeOkForOverride =
                if (overriddenDeclaration is FirPropertySymbol && overriddenDeclaration.isVar)
                    AbstractTypeChecker.equalTypes(typeCheckerContext, overridingReturnType, overriddenReturnType)
                else
                    AbstractTypeChecker.isSubtypeOf(typeCheckerContext, overridingReturnType, overriddenReturnType)

            if (!isReturnTypeOkForOverride) {
                return overriddenDeclaration
            }
        }

        return null
    }

    private fun FirCallableSymbol<*>.checkExposingGetter(
        overriddenSymbols: List<FirCallableSymbol<*>>,
        typeCheckerContext: AbstractTypeCheckerContext,
        context: CheckerContext,
    ): Triple<FirPropertyAccessorSymbol, ConeKotlinType, ConeKotlinType>? {
        val exposingGetter = this.getExposingGetter()
            ?: return null

        val exposingGetterReturnType = exposingGetter.resolvedReturnTypeRef.coneType

        // Don't report *_ON_OVERRIDE diagnostics according to an error return type. That should be reported separately.
        if (exposingGetterReturnType is ConeKotlinErrorType) {
            return null
        }

        val superExposingGetters = overriddenSymbols.mapNotNull {
            it.getExposingGetter()
        }

        for (getter in superExposingGetters) {
            val superExposingGetterReturnType = context.returnTypeCalculator.tryCalculateReturnType(getter)
                .coneType
                .upperBoundIfFlexible()
                .substituteAllTypeParameters(this, getter, context)

            if (superExposingGetterReturnType is ConeKotlinErrorType) {
                continue
            }

            val isReturnTypeOkForOverride = AbstractTypeChecker.isSubtypeOf(
                typeCheckerContext,
                exposingGetterReturnType,
                superExposingGetterReturnType
            )

            if (!isReturnTypeOkForOverride) {
                return Triple(exposingGetter, exposingGetterReturnType, superExposingGetterReturnType)
            }
        }

        return null
    }

    private fun checkMember(
        member: FirCallableSymbol<*>,
        containingClass: FirClass,
        reporter: DiagnosticReporter,
        typeCheckerContext: AbstractTypeCheckerContext,
        firTypeScope: FirTypeScope,
        context: CheckerContext,
    ) {
        val overriddenMemberSymbols = firTypeScope.retrieveDirectOverriddenOf(member)

        if (!member.isOverride) {
            if (overriddenMemberSymbols.isEmpty() ||
                context.session.overridesBackwardCompatibilityHelper.overrideCanBeOmitted(overriddenMemberSymbols, context)
            ) {
                return
            }
            val kind = member.source?.kind
            // Only report if the current member has real source or it's a member property declared inside the primary constructor.

            if (kind is FirFakeSourceElementKind.DataClassGeneratedMembers) {
                overriddenMemberSymbols.find { it.isFinal }?.let { base ->
                    reporter.reportOn(
                        containingClass.source,
                        FirErrors.DATA_CLASS_OVERRIDE_CONFLICT,
                        member,
                        base,
                        context
                    )
                }
                return
            }

            if (kind !is FirRealSourceElementKind && kind !is FirFakeSourceElementKind.PropertyFromParameter) return

            val overridden = overriddenMemberSymbols.first().originalOrSelf()
            val originalContainingClassSymbol = overridden.containingClass()?.toSymbol(context.session) as? FirRegularClassSymbol ?: return
            reporter.reportOn(
                member.source,
                FirErrors.VIRTUAL_MEMBER_HIDDEN,
                member,
                originalContainingClassSymbol,
                context
            )
            return
        }

        if (overriddenMemberSymbols.isEmpty()) {
            reporter.reportNothingToOverride(member, context)
            return
        }

        checkModality(overriddenMemberSymbols)?.let {
            reporter.reportOverridingFinalMember(member, it, context)
        }

        if (member is FirPropertySymbol) {
            member.checkMutability(overriddenMemberSymbols)?.let {
                reporter.reportVarOverriddenByVal(member, it, context)
            }
        }

        member.checkVisibility(containingClass, reporter, overriddenMemberSymbols, context)

        member.checkReturnType(
            overriddenSymbols = overriddenMemberSymbols,
            typeCheckerContext = typeCheckerContext,
            context = context,
        )?.let { restriction ->
            when (member) {
                is FirNamedFunctionSymbol -> reporter.reportReturnTypeMismatchOnFunction(member, restriction, context)
                is FirPropertySymbol -> {
                    if (member.isVar) {
                        reporter.reportTypeMismatchOnVariable(member, restriction, context)
                    } else {
                        reporter.reportTypeMismatchOnProperty(member, restriction, context)
                    }
                }
            }
        }

        member.checkExposingGetter(
            overriddenSymbols = overriddenMemberSymbols,
            typeCheckerContext = typeCheckerContext,
            context = context,
        )?.let { (getter, actualType, requiredType) ->
            reporter.reportTypeMismatchOnPropertyGetter(getter, actualType, requiredType, context)
        }
    }

    private fun DiagnosticReporter.reportNothingToOverride(declaration: FirCallableSymbol<*>, context: CheckerContext) {
        reportOn(declaration.source, FirErrors.NOTHING_TO_OVERRIDE, declaration, context)
    }

    private fun DiagnosticReporter.reportOverridingFinalMember(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>,
        context: CheckerContext
    ) {
        overridden.containingClass()?.let { containingClass ->
            reportOn(overriding.source, FirErrors.OVERRIDING_FINAL_MEMBER, overridden, containingClass.name, context)
        }
    }

    private fun DiagnosticReporter.reportVarOverriddenByVal(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>,
        context: CheckerContext
    ) {
        reportOn(overriding.source, FirErrors.VAR_OVERRIDDEN_BY_VAL, overriding, overridden, context)
    }

    private fun DiagnosticReporter.reportIncompletePropertyOverride(
        requiredVisibility: Visibility,
        overriding: FirCallableSymbol<*>,
        context: CheckerContext
    ) {
        reportOn(
            overriding.source,
            FirErrors.INCOMPLETE_PROPERTY_OVERRIDE,
            requiredVisibility,
            overriding.visibility,
            context
        )
    }

    private fun DiagnosticReporter.reportCannotWeakenAccessPrivilege(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>,
        context: CheckerContext
    ) {
        val containingClass = overridden.containingClass() ?: return
        reportOn(
            overriding.source,
            FirErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE,
            overriding.visibility,
            overridden,
            containingClass.name,
            context
        )
    }

    private fun DiagnosticReporter.reportCannotChangeAccessPrivilege(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>,
        context: CheckerContext
    ) {
        val containingClass = overridden.containingClass() ?: return
        reportOn(
            overriding.source,
            FirErrors.CANNOT_CHANGE_ACCESS_PRIVILEGE,
            overriding.visibility,
            overridden,
            containingClass.name,
            context
        )
    }

    private fun DiagnosticReporter.reportReturnTypeMismatchOnFunction(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>,
        context: CheckerContext
    ) {
        reportOn(overriding.source, FirErrors.RETURN_TYPE_MISMATCH_ON_OVERRIDE, overriding, overridden, context)
    }

    private fun DiagnosticReporter.reportTypeMismatchOnProperty(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>,
        context: CheckerContext
    ) {
        reportOn(overriding.source, FirErrors.PROPERTY_TYPE_MISMATCH_ON_OVERRIDE, overriding, overridden, context)
    }

    private fun DiagnosticReporter.reportTypeMismatchOnPropertyGetter(
        getter: FirCallableSymbol<*>,
        actual: ConeKotlinType,
        required: ConeKotlinType,
        context: CheckerContext
    ) {
        reportOn(getter.source, FirErrors.PROPERTY_GETTER_TYPE_MISMATCH_ON_OVERRIDE, actual, required, context)
    }

    private fun DiagnosticReporter.reportTypeMismatchOnVariable(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>,
        context: CheckerContext
    ) {
        reportOn(overriding.source, FirErrors.VAR_TYPE_MISMATCH_ON_OVERRIDE, overriding, overridden, context)
    }
}
