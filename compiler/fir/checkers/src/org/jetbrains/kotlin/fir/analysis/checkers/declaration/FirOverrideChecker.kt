/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.Experimentality
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.overridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.scopes.retrieveDirectOverriddenOf
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState

object FirOverrideChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val typeCheckerState = context.session.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )

        val firTypeScope = declaration.unsubstitutedScope(context)

        for (it in declaration.declarations) {
            if (it is FirSimpleFunction || it is FirProperty) {
                val callable = it as FirCallableDeclaration
                checkMember(callable.symbol, declaration, reporter, typeCheckerState, firTypeScope, context)
            }
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
            if (overridden.modality == Modality.FINAL) {
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

    private fun FirCallableSymbol<*>.checkVisibility(
        containingClass: FirClass,
        reporter: DiagnosticReporter,
        overriddenSymbols: List<FirCallableSymbol<*>>,
        context: CheckerContext
    ) {
        if (overriddenSymbols.isEmpty()) return
        val visibilities = overriddenSymbols.map {
            it to it.visibility
        }.sortedBy { pair ->
            // Regard `null` compare as Int.MIN so that we can report CANNOT_CHANGE_... first deterministically
            Visibilities.compare(visibility, pair.second) ?: Int.MIN_VALUE
        }

        if (this is FirPropertySymbol) {
            getterSymbol?.checkVisibility(
                containingClass,
                reporter,
                overriddenSymbols.map { (it as FirPropertySymbol).getterSymbol ?: it },
                context
            )
            setterSymbol?.checkVisibility(
                containingClass,
                reporter,
                overriddenSymbols.mapNotNull { (it as FirPropertySymbol).setterSymbol },
                context
            )
        } else {
            for ((overridden, overriddenVisibility) in visibilities) {
                val compare = Visibilities.compare(visibility, overriddenVisibility)
                if (compare == null) {
                    reporter.reportCannotChangeAccessPrivilege(this, overridden, context)
                    break
                } else if (compare < 0) {
                    reporter.reportCannotWeakenAccessPrivilege(this, overridden, context)
                    break
                }
            }
        }

        if (this is FirPropertyAccessorSymbol) return
        val file = context.containingFile ?: return
        val containingDeclarations = context.containingDeclarations + containingClass
        val visibilityChecker = context.session.visibilityChecker
        val hasVisibleBase = overriddenSymbols.any {
            it.lazyResolveToPhase(FirResolvePhase.STATUS)
            @OptIn(SymbolInternals::class)
            val fir = it.fir
            visibilityChecker.isVisible(
                fir,
                context.session,
                file,
                containingDeclarations,
                dispatchReceiver = null,
                skipCheckForContainingClassVisibility = true
            )
        }
        if (!hasVisibleBase) {
            //NB: Old FE reports this in an attempt to override private member,
            //while the new FE doesn't treat super's private members as overridable, so you won't get them here
            //instead you will get NOTHING_TO_OVERRIDE, which seems acceptable
            reporter.reportOn(source, FirErrors.CANNOT_OVERRIDE_INVISIBLE_MEMBER, this, overriddenSymbols.first(), context)
        }
    }

    private fun FirCallableSymbol<*>.checkDeprecation(
        reporter: DiagnosticReporter,
        overriddenSymbols: List<FirCallableSymbol<*>>,
        context: CheckerContext
    ) {
        val ownDeprecation = this.getDeprecation(context.session.languageVersionSettings)
        if (ownDeprecation == null || ownDeprecation.isNotEmpty()) return
        for (overriddenSymbol in overriddenSymbols) {
            val deprecationInfoFromOverridden = overriddenSymbol.getDeprecation(context.session.languageVersionSettings)
                ?: continue
            val deprecationFromOverriddenSymbol = deprecationInfoFromOverridden.all
                ?: deprecationInfoFromOverridden.bySpecificSite?.values?.firstOrNull()
                ?: continue
            reporter.reportOn(source, FirErrors.OVERRIDE_DEPRECATION, overriddenSymbol, deprecationFromOverriddenSymbol, context)
            return
        }
    }

    // See [OverrideResolver#isReturnTypeOkForOverride]
    private fun FirCallableSymbol<*>.checkReturnType(
        overriddenSymbols: List<FirCallableSymbol<*>>,
        typeCheckerState: TypeCheckerState,
        context: CheckerContext,
    ): FirCallableSymbol<*>? {
        val overridingReturnType = resolvedReturnTypeRef.coneType

        // Don't report *_ON_OVERRIDE diagnostics according to an error return type. That should be reported separately.
        if (overridingReturnType is ConeErrorType) {
            return null
        }

        val bounds = overriddenSymbols.map { context.returnTypeCalculator.tryCalculateReturnType(it).coneType }

        for (it in bounds.indices) {
            val overriddenDeclaration = overriddenSymbols[it]

            val overriddenReturnType = bounds[it].substituteAllTypeParameters(this, overriddenDeclaration, context)

            val isReturnTypeOkForOverride =
                if (overriddenDeclaration is FirPropertySymbol && overriddenDeclaration.isVar)
                    AbstractTypeChecker.equalTypes(typeCheckerState, overridingReturnType, overriddenReturnType)
                else
                    AbstractTypeChecker.isSubtypeOf(typeCheckerState, overridingReturnType, overriddenReturnType)

            if (!isReturnTypeOkForOverride) {
                return overriddenDeclaration
            }
        }

        return null
    }

    @OptIn(SymbolInternals::class)
    private fun FirFunctionSymbol<*>.checkDefaultValues(
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        for (valueParameterSymbol in valueParameterSymbols) {
            val defaultValue = valueParameterSymbol.fir.defaultValue
            if (defaultValue != null) {
                reporter.reportOn(defaultValue.source, FirErrors.DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE, context)
            }
        }
    }

    private fun FirCallableSymbol<*>.checkDataClassCopy(
        reporter: DiagnosticReporter,
        overriddenMemberSymbols: List<FirCallableSymbol<*>>,
        containingClass: FirClass,
        context: CheckerContext,
    ) {
        val overridden = overriddenMemberSymbols.firstOrNull() ?: return
        val overriddenClass = overridden.getContainingClassSymbol(context.session) as? FirClassSymbol<*> ?: return
        reporter.reportOn(containingClass.source, FirErrors.DATA_CLASS_OVERRIDE_DEFAULT_VALUES, this, overriddenClass, context)
    }

    private fun checkMember(
        member: FirCallableSymbol<*>,
        containingClass: FirClass,
        reporter: DiagnosticReporter,
        typeCheckerState: TypeCheckerState,
        firTypeScope: FirTypeScope,
        context: CheckerContext
    ) {
        val overriddenMemberSymbols = firTypeScope.retrieveDirectOverriddenOf(member)
        val hasOverrideKeyword = member.hasModifier(KtTokens.OVERRIDE_KEYWORD)

        if (!member.isOverride || !hasOverrideKeyword) {
            if (overriddenMemberSymbols.isEmpty() ||
                context.session.overridesBackwardCompatibilityHelper.overrideCanBeOmitted(overriddenMemberSymbols, context)
            ) {
                return
            }
            val kind = member.source?.kind
            // Only report if the current member has real source or it's a member property declared inside the primary constructor.

            if (kind is KtFakeSourceElementKind.DataClassGeneratedMembers) {
                overriddenMemberSymbols.find { it.isFinal }?.let { base ->
                    reporter.reportOn(
                        containingClass.source,
                        FirErrors.DATA_CLASS_OVERRIDE_CONFLICT,
                        member,
                        base,
                        context
                    )
                }
                if (member.name == StandardNames.DATA_CLASS_COPY) {
                    member.checkDataClassCopy(reporter, overriddenMemberSymbols, containingClass, context)
                }
                return
            }

            if (kind !is KtRealSourceElementKind && kind !is KtFakeSourceElementKind.PropertyFromParameter) return

            val visibilityChecker = context.session.visibilityChecker
            val file = context.containingFile ?: return
            val containingDeclarations = context.containingDeclarations + containingClass

            @OptIn(SymbolInternals::class)
            val overridden = overriddenMemberSymbols.firstOrNull {
                it.lazyResolveToPhase(FirResolvePhase.STATUS)
                visibilityChecker.isVisible(
                    it.originalOrSelf().fir,
                    context.session,
                    file,
                    containingDeclarations,
                    dispatchReceiver = null,
                    skipCheckForContainingClassVisibility = true
                )
            }?.originalOrSelf() ?: return
            val originalContainingClassSymbol = overridden.containingClassLookupTag()?.toSymbol(context.session) as? FirRegularClassSymbol ?: return
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

        checkOverriddenExperimentalities(member, overriddenMemberSymbols, context, reporter)

        checkModality(overriddenMemberSymbols)?.let {
            reporter.reportOverridingFinalMember(member, it, context)
        }

        if (member is FirPropertySymbol) {
            member.checkMutability(overriddenMemberSymbols)?.let {
                reporter.reportVarOverriddenByVal(member, it, context)
            }
        }

        member.checkVisibility(containingClass, reporter, overriddenMemberSymbols, context)

        member.checkDeprecation(reporter, overriddenMemberSymbols, context)

        if (member is FirFunctionSymbol) {
            member.checkDefaultValues(reporter, context)
        }

        val restriction = member.checkReturnType(
            overriddenSymbols = overriddenMemberSymbols,
            typeCheckerState = typeCheckerState,
            context = context,
        ) ?: return
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

    @OptIn(SymbolInternals::class)
    private fun checkOverriddenExperimentalities(
        memberSymbol: FirCallableSymbol<*>,
        overriddenMemberSymbols: List<FirCallableSymbol<*>>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        with(FirOptInUsageBaseChecker) {
            val overriddenExperimentalities = mutableSetOf<Experimentality>()
            val session = context.session
            val overriddenSymbolsWithUnwrappedIntersectionOverrides = overriddenMemberSymbols.flatMap {
                when (it) {
                    is FirIntersectionOverridePropertySymbol -> it.intersections
                    is FirIntersectionOverrideFunctionSymbol -> it.intersections
                    else -> listOf(it)
                }
            }
            for (overriddenMemberSymbol in overriddenSymbolsWithUnwrappedIntersectionOverrides) {
                overriddenMemberSymbol.loadExperimentalitiesFromAnnotationTo(session, overriddenExperimentalities)
            }
            reportNotAcceptedOverrideExperimentalities(
                overriddenExperimentalities, memberSymbol, context, reporter
            )
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
        overridden.containingClassLookupTag()?.let { containingClass ->
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

    private fun DiagnosticReporter.reportCannotWeakenAccessPrivilege(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>,
        context: CheckerContext
    ) {
        val containingClass = overridden.containingClassLookupTag() ?: return
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
        val containingClass = overridden.containingClassLookupTag() ?: return
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

    private fun DiagnosticReporter.reportTypeMismatchOnVariable(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>,
        context: CheckerContext
    ) {
        reportOn(overriding.source, FirErrors.VAR_TYPE_MISMATCH_ON_OVERRIDE, overriding, overridden, context)
    }
}
