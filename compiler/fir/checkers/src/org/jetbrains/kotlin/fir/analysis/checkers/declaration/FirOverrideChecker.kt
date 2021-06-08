/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.overridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.upperBoundIfFlexible
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirOverrideChecker : FirClassChecker() {
    override fun check(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        val typeCheckerContext = context.session.typeContext.newBaseTypeCheckerContext(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )

        val firTypeScope = declaration.unsubstitutedScope(context)

        for (it in declaration.declarations) {
            if (it is FirSimpleFunction || it is FirProperty) {
                checkMember(it as FirCallableMemberDeclaration<*>, reporter, typeCheckerContext, firTypeScope, context)
            }
        }
    }

    private fun FirTypeScope.retrieveDirectOverriddenOf(member: FirCallableMemberDeclaration<*>): List<FirCallableSymbol<*>> {
        return when (member) {
            is FirSimpleFunction -> {
                processFunctionsByName(member.name) {}
                getDirectOverriddenFunctions(member.symbol)
            }
            is FirProperty -> {
                processPropertiesByName(member.name) {}
                getDirectOverriddenProperties(member.symbol)
            }
            else -> throw IllegalArgumentException("unexpected member kind $member")
        }
    }

    private fun ConeKotlinType.substituteAllTypeParameters(
        overrideDeclaration: FirCallableMemberDeclaration<*>,
        baseDeclaration: FirCallableDeclaration<*>,
        context: CheckerContext
    ): ConeKotlinType {
        if (overrideDeclaration.typeParameters.isEmpty()) {
            return this
        }

        val parametersOwner = baseDeclaration.safeAs<FirTypeParametersOwner>()
            ?: return this

        val map = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
        val size = minOf(overrideDeclaration.typeParameters.size, parametersOwner.typeParameters.size)

        for (it in 0 until size) {
            val to = overrideDeclaration.typeParameters[it]
            val from = parametersOwner.typeParameters[it]

            map[from.symbol] = to.toConeType()
        }

        return substitutorByMap(map, context.session).substituteOrSelf(this)
    }

    private fun checkModality(
        overriddenSymbols: List<FirCallableSymbol<*>>,
    ): FirCallableDeclaration<*>? {
        for (overridden in overriddenSymbols) {
            if (overridden.fir !is FirMemberDeclaration) continue
            val modality = (overridden.fir as FirMemberDeclaration).status.modality
            val isEffectivelyFinal = modality == null || modality == Modality.FINAL
            if (isEffectivelyFinal) {
                return overridden.fir
            }
        }
        return null
    }

    private fun FirProperty.checkMutability(
        overriddenSymbols: List<FirCallableSymbol<*>>,
    ): FirMemberDeclaration? {
        if (isVar) return null
        return overriddenSymbols.find { (it.fir as? FirProperty)?.isVar == true }?.fir?.safeAs()
    }

    private fun FirCallableMemberDeclaration<*>.checkVisibility(
        reporter: DiagnosticReporter,
        overriddenSymbols: List<FirCallableSymbol<*>>,
        context: CheckerContext
    ) {
        val visibilities = overriddenSymbols.mapNotNull {
            if (it.fir !is FirMemberDeclaration) return@mapNotNull null
            it to (it.fir as FirMemberDeclaration).visibility
        }.sortedBy { pair ->
            // Regard `null` compare as Int.MIN so that we can report CANNOT_CHANGE_... first deterministically
            Visibilities.compare(visibility, pair.second) ?: Int.MIN_VALUE
        }

        for ((overridden, overriddenVisibility) in visibilities) {
            val compare = Visibilities.compare(visibility, overriddenVisibility)
            if (compare == null) {
                reporter.reportCannotChangeAccessPrivilege(this, overridden.fir, context)
                return
            } else if (compare < 0) {
                reporter.reportCannotWeakenAccessPrivilege(this, overridden.fir, context)
                return
            }
        }
    }

    // See [OverrideResolver#isReturnTypeOkForOverride]
    private fun FirCallableMemberDeclaration<*>.checkReturnType(
        overriddenSymbols: List<FirCallableSymbol<*>>,
        typeCheckerContext: AbstractTypeCheckerContext,
        context: CheckerContext,
    ): FirMemberDeclaration? {
        val overridingReturnType = returnTypeRef.coneType

        // Don't report *_ON_OVERRIDE diagnostics according to an error return type. That should be reported separately.
        if (overridingReturnType is ConeKotlinErrorType) {
            return null
        }

        val bounds = overriddenSymbols.map { context.returnTypeCalculator.tryCalculateReturnType(it.fir).coneType.upperBoundIfFlexible() }

        for (it in bounds.indices) {
            val overriddenDeclaration = overriddenSymbols[it].fir

            val overriddenReturnType = bounds[it].substituteAllTypeParameters(this, overriddenDeclaration, context)

            val isReturnTypeOkForOverride =
                if (overriddenDeclaration is FirProperty && overriddenDeclaration.isVar)
                    AbstractTypeChecker.equalTypes(typeCheckerContext, overridingReturnType, overriddenReturnType)
                else
                    AbstractTypeChecker.isSubtypeOf(typeCheckerContext, overridingReturnType, overriddenReturnType)

            if (!isReturnTypeOkForOverride) {
                return overriddenDeclaration.safeAs()
            }
        }

        return null
    }

    private fun checkMember(
        member: FirCallableMemberDeclaration<*>,
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
            if (kind !is FirRealSourceElementKind && kind !is FirFakeSourceElementKind.PropertyFromParameter) return

            val overridden = overriddenMemberSymbols.first().originalOrSelf()
            val containingClass = overridden.containingClass()?.toFirRegularClass(context.session) ?: return
            reporter.reportOn(
                member.source,
                FirErrors.VIRTUAL_MEMBER_HIDDEN,
                member,
                containingClass,
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

        if (member is FirProperty) {
            member.checkMutability(overriddenMemberSymbols)?.let {
                reporter.reportVarOverriddenByVal(member, it, context)
            }
        }

        member.checkVisibility(reporter, overriddenMemberSymbols, context)

        val restriction = member.checkReturnType(
            overriddenSymbols = overriddenMemberSymbols,
            typeCheckerContext = typeCheckerContext,
            context = context,
        ) ?: return
        when (member) {
            is FirSimpleFunction -> reporter.reportReturnTypeMismatchOnFunction(member, restriction, context)
            is FirProperty -> {
                if (member.isVar) {
                    reporter.reportTypeMismatchOnVariable(member, restriction, context)
                } else {
                    reporter.reportTypeMismatchOnProperty(member, restriction, context)
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER") // TODO: delete me after implementing body
    private fun DiagnosticReporter.reportNothingToOverride(declaration: FirMemberDeclaration, context: CheckerContext) {
        // TODO: not ready yet, e.g., Collections
        // reportOn(declaration.source, FirErrors.NOTHING_TO_OVERRIDE, declaration, context)
    }

    private fun DiagnosticReporter.reportOverridingFinalMember(
        overriding: FirMemberDeclaration,
        overridden: FirCallableDeclaration<*>,
        context: CheckerContext
    ) {
        overridden.containingClass()?.let { containingClass ->
            reportOn(overriding.source, FirErrors.OVERRIDING_FINAL_MEMBER, overridden, containingClass.name, context)
        }
    }

    private fun DiagnosticReporter.reportVarOverriddenByVal(
        overriding: FirMemberDeclaration,
        overridden: FirMemberDeclaration,
        context: CheckerContext
    ) {
        reportOn(overriding.source, FirErrors.VAR_OVERRIDDEN_BY_VAL, overriding, overridden, context)
    }

    private fun DiagnosticReporter.reportCannotWeakenAccessPrivilege(
        overriding: FirMemberDeclaration,
        overridden: FirCallableDeclaration<*>,
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
        overriding: FirMemberDeclaration,
        overridden: FirCallableDeclaration<*>,
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
        overriding: FirMemberDeclaration,
        overridden: FirMemberDeclaration,
        context: CheckerContext
    ) {
        reportOn(overriding.source, FirErrors.RETURN_TYPE_MISMATCH_ON_OVERRIDE, overriding, overridden, context)
    }

    private fun DiagnosticReporter.reportTypeMismatchOnProperty(
        overriding: FirMemberDeclaration,
        overridden: FirMemberDeclaration,
        context: CheckerContext
    ) {
        reportOn(overriding.source, FirErrors.PROPERTY_TYPE_MISMATCH_ON_OVERRIDE, overriding, overridden, context)
    }

    private fun DiagnosticReporter.reportTypeMismatchOnVariable(
        overriding: FirMemberDeclaration,
        overridden: FirMemberDeclaration,
        context: CheckerContext
    ) {
        reportOn(overriding.source, FirErrors.VAR_TYPE_MISMATCH_ON_OVERRIDE, overriding, overridden, context)
    }
}
