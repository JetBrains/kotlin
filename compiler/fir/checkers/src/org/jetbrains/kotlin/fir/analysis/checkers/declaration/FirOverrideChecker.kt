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
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirDeprecationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.Experimentality
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.overridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.CallToPotentiallyHiddenSymbolResult.VisibleWithDeprecation
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.scopes.retrieveDirectOverriddenOf
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.deprecation.SimpleDeprecationInfo
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState

abstract class FirAbstractOverrideChecker(mppKind: MppCheckerKind) : FirClassChecker(mppKind) {
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

    // See [OverrideResolver#isReturnTypeOkForOverride]
    protected fun FirCallableSymbol<*>.checkReturnType(
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
}

sealed class FirOverrideChecker(mppKind: MppCheckerKind) : FirAbstractOverrideChecker(mppKind) {
    object Regular : FirOverrideChecker(MppCheckerKind.Platform) {
        override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    object ForExpectClass : FirOverrideChecker(MppCheckerKind.Common) {
        override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (!declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val typeCheckerState = context.session.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )

        val firTypeScope = declaration.unsubstitutedScope(context)

        // Types from substitution overrides may not be compatible with the types before substitution due to variance.
        // For example, there is `Enum<E>::getDeclaringClass()` that returns `Class<E>`, and we may create a SO
        // `MyEnum::getDeclaringClass()` returning `Class<MyEnum>`, but `Class<T>` is invariant w.r.t. `T`.
        // Or we can also create a substitution override for a final function.
        // Since substitution overrides are allowed to be incorrect overrides, they are skipped below.

        // Other kinds of fake overrides may also be incorrect, but not to that extent, so we can
        // check them more granularly. See the relevant comments.

        firTypeScope.processAllProperties {
            if (it.containingClassLookupTag() == declaration.symbol.toLookupTag() && !it.isSubstitutionOverride) {
                checkMember(it, declaration, reporter, typeCheckerState, firTypeScope, context)
            }
        }

        firTypeScope.processAllFunctions {
            if (it.containingClassLookupTag() == declaration.symbol.toLookupTag() && !it.isSubstitutionOverride) {
                checkMember(it, declaration, reporter, typeCheckerState, firTypeScope, context)
            }
        }
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
        if (visibility == Visibilities.Unknown) {
            // MANY_*_NOT_IMPLEMENTED implies CANNOT_INFER_VISIBILITY as per KT-63741
            val isManyNotImplementedDiagnosticReported = overriddenSymbols.count { !it.isAbstract } != 1
            val isSimpleToReasonAboutSituation = overriddenSymbols.none { it.isIntersectionOverride }
            if (!(isManyNotImplementedDiagnosticReported && isSimpleToReasonAboutSituation)) {
                reporter.reportOn(source, FirErrors.CANNOT_INFER_VISIBILITY, this, context)
            }
            return
        }

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
        context: CheckerContext,
        firTypeScope: FirTypeScope,
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

        if (this is FirNamedFunctionSymbol) {
            val callableName = this.name.asString()

            if (callableName in FirDeprecationChecker.DeprecatedOverrideOfHiddenReplacements) {
                firTypeScope.processOverriddenFunctions(this) {
                    if (it.hiddenStatusOfCall(isSuperCall = false, isCallToOverride = true) == VisibleWithDeprecation) {
                        val message = FirDeprecationChecker.getDeprecatedOverrideOfHiddenMessage(callableName)
                        val deprecationInfo =
                            SimpleDeprecationInfo(DeprecationLevelValue.WARNING, propagatesToOverrides = false, message)
                        reporter.reportOn(source, FirErrors.OVERRIDE_DEPRECATION, it, deprecationInfo, context)
                        return@processOverriddenFunctions ProcessorAction.STOP
                    }

                    ProcessorAction.NEXT
                }
            }
        }
    }

    private fun FirFunctionSymbol<*>.checkDefaultValues(
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        for (valueParameterSymbol in valueParameterSymbols) {
            if (valueParameterSymbol.hasDefaultValue) {
                reporter.reportOn(valueParameterSymbol.defaultValueSource, FirErrors.DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE, context)
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
        val isOverride = member.isOverride && (member.origin != FirDeclarationOrigin.Source || hasOverrideKeyword)

        if (!isOverride) {
            if (overriddenMemberSymbols.isEmpty() ||
                context.session.overridesBackwardCompatibilityHelper.overrideCanBeOmitted(overriddenMemberSymbols, context)
            ) {
                return
            }
            val kind = member.source?.kind
            // Only report if the current member has real source or it's a member property declared inside the primary constructor.
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

        if (member.source?.kind is KtFakeSourceElementKind.DataClassGeneratedMembers) {
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

        if (overriddenMemberSymbols.isEmpty()) {
            reporter.reportNothingToOverride(member, context)
            return
        }

        checkOverriddenExperimentalities(member, overriddenMemberSymbols, context, reporter)

        // The compiler may generate an intersection override for a case where in the resulting
        // JVM bytecode there would be no override (a superclass implicitly overrides a function from a superinterface).
        // The superclass function may be final, which would render the IO invalid.
        // Delegated are handled by `OVERRIDING_FINAL_MEMBER_BY_DELEGATION`
        if (!member.isIntersectionOverride && !member.isDelegated) {
            checkModality(overriddenMemberSymbols)?.let {
                reporter.reportOverridingFinalMember(member, it, context)
            }
        }

        // Delegated members are checked by `VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION`
        if (member is FirPropertySymbol && !member.isDelegated) {
            member.checkMutability(overriddenMemberSymbols)?.let {
                reporter.reportVarOverriddenByVal(member, it, context)
            }
        }

        member.checkVisibility(containingClass, reporter, overriddenMemberSymbols, context)

        if (member.origin == FirDeclarationOrigin.Source) {
            member.checkDeprecation(reporter, overriddenMemberSymbols, context, firTypeScope)
        }

        // Data class members are already checked by `DATA_CLASS_OVERRIDE_DEFAULT_VALUES`
        if (member is FirFunctionSymbol && member.origin != FirDeclarationOrigin.Synthetic.DataClassMember) {
            member.checkDefaultValues(reporter, context)
        }

        // These cases are checked separately by diagnostics like
        // `RETURN_TYPE_MISMATCH_ON_INHERITANCE`
        if (member.isIntersectionOverride || member.isDelegated) {
            return
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
