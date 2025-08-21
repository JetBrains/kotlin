/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirDeprecationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.Experimentality
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.processOverriddenFunctionsWithActionSafe
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.overridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.CallToPotentiallyHiddenSymbolResult.VisibleWithDeprecation
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState

abstract class FirAbstractOverrideChecker(mppKind: MppCheckerKind) : FirClassChecker(mppKind) {
    context(context: CheckerContext)
    private fun ConeKotlinType.substituteAllTypeParameters(
        overrideDeclaration: FirCallableSymbol<*>,
        baseDeclaration: FirCallableSymbol<*>
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
    context(context: CheckerContext)
    protected fun FirCallableSymbol<*>.checkReturnType(
        overriddenSymbols: List<FirCallableSymbol<*>>,
        typeCheckerState: TypeCheckerState,
    ): FirCallableSymbol<*>? {
        val overridingReturnType = resolvedReturnTypeRef.coneType

        // Don't report *_ON_OVERRIDE diagnostics according to an error return type. That should be reported separately.
        if (overridingReturnType is ConeErrorType) {
            return null
        }

        val bounds = overriddenSymbols.map { context.returnTypeCalculator.tryCalculateReturnType(it).coneType }

        for (it in bounds.indices) {
            val overriddenDeclaration = overriddenSymbols[it]

            val overriddenReturnType = bounds[it].substituteAllTypeParameters(this, overriddenDeclaration)

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
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirClass) {
            if (declaration.isExpect) return
            super.check(declaration)
        }
    }

    object ForExpectClass : FirOverrideChecker(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirClass) {
            if (!declaration.isExpect) return
            super.check(declaration)
        }
    }

    private val consideredOrigins: Set<FirDeclarationOrigin> = setOf(
        FirDeclarationOrigin.Source,
        FirDeclarationOrigin.Synthetic.DataClassMember,
        FirDeclarationOrigin.Delegated,
        FirDeclarationOrigin.IntersectionOverride,
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        // Don't report override-related problems if we have bad supertypes
        if (declaration.superTypeRefs.any { it is FirErrorTypeRef }) return

        val typeCheckerState = context.session.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false,
            dnnTypesEqualToFlexible = LanguageFeature.AllowDnnTypeOverridingFlexibleType.isEnabled()
        )

        val firTypeScope = declaration.unsubstitutedScope()

        // Types from substitution overrides may not be compatible with the types before substitution due to variance.
        // For example, there is `Enum<E>::getDeclaringClass()` that returns `Class<E>`, and we may create a SO
        // `MyEnum::getDeclaringClass()` returning `Class<MyEnum>`, but `Class<T>` is invariant w.r.t. `T`.
        // Or we can also create a substitution override for a final function.
        // Since substitution overrides are allowed to be incorrect overrides, they are skipped below.

        // Other kinds of fake overrides may also be incorrect, but not to that extent, so we can
        // check them more granularly. See the relevant comments.

        fun checkMember(it: FirCallableSymbol<*>) {
            val isFromThis = it.origin in consideredOrigins && it.containingClassLookupTag() == declaration.symbol.toLookupTag()

            if (isFromThis && !it.isSubstitutionOverride) {
                checkMember(it, declaration, typeCheckerState, firTypeScope)
            } else {
                val source = it.source?.takeIf { isFromThis } ?: declaration.source
                it.ensureKnownVisibility(source)
            }
        }

        firTypeScope.processAllProperties(::checkMember)
        firTypeScope.processAllFunctions(::checkMember)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    /**
     * Returns `false` if [Visibilities.Unknown].
     */
    private fun FirCallableSymbol<*>.ensureKnownVisibility(
        source: KtSourceElement? = this.source,
    ) = when {
        visibility != Visibilities.Unknown -> true
        else -> false.also { reporter.reportOn(source, chooseCannotInferVisibilityFor(this), this) }
    }

    private fun chooseCannotInferVisibilityFor(symbol: FirCallableSymbol<*>) = when {
        !symbol.wouldMissDiagnosticInK1 -> FirErrors.CANNOT_INFER_VISIBILITY
        else -> FirErrors.CANNOT_INFER_VISIBILITY_WARNING
    }

    private fun chooseCannotChangeAccessPrivilegeFor(symbol: FirCallableSymbol<*>) = when {
        !symbol.wouldMissDiagnosticInK1 -> FirErrors.CANNOT_CHANGE_ACCESS_PRIVILEGE
        else -> FirErrors.CANNOT_CHANGE_ACCESS_PRIVILEGE_WARNING
    }

    private fun chooseCannotWeakenAccessPrivilegeFor(symbol: FirCallableSymbol<*>) = when {
        !symbol.wouldMissDiagnosticInK1 -> FirErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE
        else -> FirErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING
    }

    private val FirCallableSymbol<*>.wouldMissDiagnosticInK1: Boolean
        get() = this is FirPropertyAccessorSymbol && propertySymbol.isIntersectionOverride && visibility != propertySymbol.visibility

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

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun FirCallableSymbol<*>.checkVisibility(
        containingClass: FirClass,
        overriddenSymbols: List<FirCallableSymbol<*>>
    ) {
        if (!ensureKnownVisibility()) {
            return
        }

        if (overriddenSymbols.isEmpty()) return
        val visibilities = overriddenSymbols.map {
            it to it.visibility
        }.sortedBy { pair ->
            // Regard `null` compare as Int.MIN so that we can report CANNOT_CHANGE_... first deterministically
            Visibilities.compare(visibility, pair.second) ?: Int.MIN_VALUE
        }

        if (this is FirPropertySymbol && canDelegateVisibilityConsistencyChecksToAccessors) {
            getterSymbol?.checkVisibility(
                containingClass,
                overriddenSymbols.map { (it as FirPropertySymbol).getterSymbol ?: it }
            )
            setterSymbol?.checkVisibility(
                containingClass,
                overriddenSymbols.mapNotNull { (it as FirPropertySymbol).setterSymbol }
            )
        } else {
            for ((overridden, overriddenVisibility) in visibilities) {
                val compare = Visibilities.compare(visibility, overriddenVisibility)
                if (compare == null) {
                    reporter.reportCannotChangeAccessPrivilege(this, overridden)
                    break
                } else if (compare < 0) {
                    reporter.reportCannotWeakenAccessPrivilege(this, overridden)
                    break
                }
            }
        }

        if (this is FirPropertyAccessorSymbol) return
        val file = context.containingFileSymbol ?: return
        val containingDeclarations = context.containingDeclarations + containingClass.symbol
        val visibilityChecker = context.session.visibilityChecker
        val hasVisibleBase = overriddenSymbols.any {
            it.lazyResolveToPhase(FirResolvePhase.STATUS)
            visibilityChecker.isVisible(
                it,
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
            reporter.reportOn(source, FirErrors.CANNOT_OVERRIDE_INVISIBLE_MEMBER, this, overriddenSymbols.first())
        }
    }

    /**
     * Properties that are intersection overrides are created lightweight:
     * they only contain accessors if they have visibilities that are different
     * from the property visibility
     *
     * @see org.jetbrains.kotlin.fir.scopes.impl.FirFakeOverrideGenerator.buildCopyIfNeeded
     */
    private val FirPropertySymbol.canDelegateVisibilityConsistencyChecksToAccessors: Boolean
        get() = getterSymbol != null || setterSymbol != null

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun FirCallableSymbol<*>.checkDeprecation(
        overriddenSymbols: List<FirCallableSymbol<*>>,
    ) {
        val ownDeprecation = this.getDeprecation(context.languageVersionSettings)
        if (ownDeprecation != null && ownDeprecation.isNotEmpty()) return

        val overriddenWithDeprecation = overriddenSymbols.associateWith { overriddenSymbol ->
            overriddenSymbol.getDeprecation(context.languageVersionSettings)?.takeIf { it.isNotEmpty() }
        }.filterValues { it != null }
        /*
         * If a function overrides both deprecated and non-deprecated function, it's ok to not have the @Deprecated annotation on override.
         */
        if (overriddenWithDeprecation.size == overriddenSymbols.size) {
            for ((overriddenSymbol, deprecationInfoFromOverridden) in overriddenWithDeprecation) {
                val deprecationFromOverriddenSymbol = deprecationInfoFromOverridden!!.all
                    ?: deprecationInfoFromOverridden.bySpecificSite?.values?.firstOrNull()
                    ?: continue
                reporter.reportOn(source, FirErrors.OVERRIDE_DEPRECATION, overriddenSymbol, deprecationFromOverriddenSymbol)
                return
            }
        }

        if (this is FirNamedFunctionSymbol) {
            val callableName = this.name.asString()

            if (callableName in FirDeprecationChecker.DeprecatedOverrideOfHiddenReplacements) {
                this.processOverriddenFunctionsWithActionSafe {
                    if (it.hiddenStatusOfCall(isSuperCall = false, isCallToOverride = true) == VisibleWithDeprecation) {
                        val message = FirDeprecationChecker.getDeprecatedOverrideOfHiddenMessage(callableName)
                        val deprecationInfo = object : FirDeprecationInfo() {
                            override val deprecationLevel: DeprecationLevelValue get() = DeprecationLevelValue.WARNING
                            override val propagatesToOverrides: Boolean get() = false
                            override fun getMessage(session: FirSession): String = message
                        }
                        reporter.reportOn(source, FirErrors.OVERRIDE_DEPRECATION, it, deprecationInfo)
                        return@processOverriddenFunctionsWithActionSafe ProcessorAction.STOP
                    }

                    ProcessorAction.NEXT
                }
            }
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun FirFunctionSymbol<*>.checkDefaultValues(
    ) {
        for (valueParameterSymbol in valueParameterSymbols) {
            if (valueParameterSymbol.hasDefaultValue) {
                reporter.reportOn(valueParameterSymbol.defaultValueSource, FirErrors.DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE)
            }
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun FirCallableSymbol<*>.checkDataClassCopy(
        overriddenMemberSymbols: List<FirCallableSymbol<*>>,
        containingClass: FirClass,
    ) {
        val overridden = overriddenMemberSymbols.firstOrNull() ?: return
        val overriddenClass = overridden.getContainingClassSymbol() as? FirClassSymbol<*> ?: return
        reporter.reportOn(containingClass.source, FirErrors.DATA_CLASS_OVERRIDE_DEFAULT_VALUES, this, overriddenClass)
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun FirCallableSymbol<*>.checkSuspend(
        overriddenMemberSymbols: List<FirCallableSymbol<*>>,
        containingClass: FirClass,
    ) {
        val overriddenSymbolWithMismatch = overriddenMemberSymbols.firstOrNull { it.isSuspend != this.isSuspend } ?: return
        if (overriddenMemberSymbols.any { it.isSuspend != overriddenSymbolWithMismatch.isSuspend }) {
            reporter.reportOn(source, FirErrors.CONFLICTING_INHERITED_MEMBERS, containingClass.symbol, overriddenMemberSymbols)
            return
        }
        val error = if (this.isSuspend) FirErrors.NON_SUSPEND_OVERRIDDEN_BY_SUSPEND else FirErrors.SUSPEND_OVERRIDDEN_BY_NON_SUSPEND
        reporter.reportOn(source, error, this, overriddenSymbolWithMismatch)
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkMember(
        member: FirCallableSymbol<*>,
        containingClass: FirClass,
        typeCheckerState: TypeCheckerState,
        firTypeScope: FirTypeScope
    ) {
        val overriddenMemberSymbols = firTypeScope.getDirectOverriddenSafe(member)

        member.checkSuspend(overriddenMemberSymbols, containingClass)
        if (member.checkDataClassMembers(overriddenMemberSymbols, containingClass, typeCheckerState)) return

        if (!member.isOverride) {
            if (overriddenMemberSymbols.isEmpty() ||
                context.session.overridesBackwardCompatibilityHelper.overrideCanBeOmitted(overriddenMemberSymbols)
            ) {
                return
            }
            val kind = member.source?.kind
            // Only report if the current member has real source or it's a member property declared inside the primary constructor.
            if (kind !is KtRealSourceElementKind && kind !is KtFakeSourceElementKind.PropertyFromParameter) return

            val visibilityChecker = context.session.visibilityChecker
            val file = context.containingFileSymbol ?: return
            val containingDeclarations = context.containingDeclarations + containingClass.symbol

            val overridden = overriddenMemberSymbols.firstOrNull {
                visibilityChecker.isVisible(
                    it.originalOrSelf(),
                    context.session,
                    file,
                    containingDeclarations,
                    dispatchReceiver = null,
                    skipCheckForContainingClassVisibility = true
                )
            }?.originalOrSelf() ?: return
            val originalContainingClassSymbol = overridden.containingClassLookupTag()?.toRegularClassSymbol(context.session) ?: return
            reporter.reportOn(
                member.source,
                FirErrors.VIRTUAL_MEMBER_HIDDEN,
                member,
                originalContainingClassSymbol
            )
            return
        }

        if (overriddenMemberSymbols.isEmpty()) {
            reporter.reportNothingToOverride(member, firTypeScope)
            return
        }

        checkOverriddenExperimentalities(member, overriddenMemberSymbols)

        // The compiler may generate an intersection override for a case where in the resulting
        // JVM bytecode there would be no override (a superclass implicitly overrides a function from a superinterface).
        // The superclass function may be final, which would render the IO invalid.
        // Delegated are handled by `OVERRIDING_FINAL_MEMBER_BY_DELEGATION`
        if (!member.isIntersectionOverride && !member.isDelegated) {
            checkModality(overriddenMemberSymbols)?.let {
                reporter.reportOverridingFinalMember(member, it)
            }
        }

        // Delegated members are checked by `VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION`
        if (member is FirPropertySymbol && !member.isDelegated) {
            member.checkMutability(overriddenMemberSymbols)?.let {
                reporter.reportVarOverriddenByVal(member, it)
            }
        }

        member.checkVisibility(containingClass, overriddenMemberSymbols)

        if (member.origin == FirDeclarationOrigin.Source) {
            member.checkDeprecation(overriddenMemberSymbols)
        }

        // Data class members are already checked by `DATA_CLASS_OVERRIDE_DEFAULT_VALUES`
        if (member is FirFunctionSymbol && member.origin != FirDeclarationOrigin.Synthetic.DataClassMember) {
            member.checkDefaultValues()
        }

        // These cases are checked separately by diagnostics like
        // `RETURN_TYPE_MISMATCH_ON_INHERITANCE`
        if (member.isIntersectionOverride || member.isDelegated) {
            return
        }

        val restriction = member.checkReturnType(
            overriddenSymbols = overriddenMemberSymbols,
            typeCheckerState = typeCheckerState,
        ) ?: return
        when (member) {
            is FirNamedFunctionSymbol -> reporter.reportReturnTypeMismatchOnFunction(member, restriction)
            is FirPropertySymbol -> {
                if (member.isVar) {
                    reporter.reportTypeMismatchOnVariable(member, restriction)
                } else {
                    reporter.reportTypeMismatchOnProperty(member, restriction)
                }
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun FirCallableSymbol<*>.checkDataClassMembers(
        overriddenMemberSymbols: List<FirCallableSymbol<*>>,
        containingClass: FirClass,
        typeCheckerState: TypeCheckerState,
    ): Boolean {
        if (source?.kind is KtFakeSourceElementKind.DataClassGeneratedMembers) {
            val conflictingSymbol = overriddenMemberSymbols.find { it.isFinal } ?: checkReturnType(
                overriddenSymbols = overriddenMemberSymbols,
                typeCheckerState = typeCheckerState,
            )
            if (conflictingSymbol != null) {
                reporter.reportOn(
                    containingClass.source,
                    FirErrors.DATA_CLASS_OVERRIDE_CONFLICT,
                    this,
                    conflictingSymbol
                )
            }
            if (name == StandardNames.DATA_CLASS_COPY) {
                checkDataClassCopy(overriddenMemberSymbols, containingClass)
            }
            return true
        }
        return false
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    @OptIn(SymbolInternals::class)
    private fun checkOverriddenExperimentalities(
        memberSymbol: FirCallableSymbol<*>,
        overriddenMemberSymbols: List<FirCallableSymbol<*>>,
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
                overriddenExperimentalities, memberSymbol
            )
        }
    }

    context(context: CheckerContext)
    private fun DiagnosticReporter.reportNothingToOverride(
        declaration: FirCallableSymbol<*>,
        firTypeScope: FirTypeScope,
    ) {
        val containingClassSymbol = declaration.getContainingClassSymbol()
        val candidates = if (declaration is FirPropertySymbol) {
            firTypeScope.getProperties(declaration.name)
        } else {
            firTypeScope.getFunctions(declaration.name)
        }.filter {
            it.unwrapFakeOverrides().getContainingClassSymbol() != containingClassSymbol &&
                    (it.isOpen || it.isAbstract)
        }

        reportOn(declaration.source, FirErrors.NOTHING_TO_OVERRIDE, declaration, candidates)
    }

    context(context: CheckerContext)
    private fun DiagnosticReporter.reportOverridingFinalMember(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>
    ) {
        overridden.containingClassLookupTag()?.let { containingClass ->
            reportOn(overriding.source, FirErrors.OVERRIDING_FINAL_MEMBER, overridden, containingClass.name)
        }
    }

    context(context: CheckerContext)
    private fun DiagnosticReporter.reportVarOverriddenByVal(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>
    ) {
        reportOn(overriding.source, FirErrors.VAR_OVERRIDDEN_BY_VAL, overridden, overriding)
    }

    context(context: CheckerContext)
    private fun DiagnosticReporter.reportCannotWeakenAccessPrivilege(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>
    ) {
        val containingClass = overridden.containingClassLookupTag() ?: return
        reportOn(
            overriding.source,
            chooseCannotWeakenAccessPrivilegeFor(overriding),
            overriding.visibility,
            overridden,
            containingClass.name
        )
    }

    context(context: CheckerContext)
    private fun DiagnosticReporter.reportCannotChangeAccessPrivilege(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>
    ) {
        val containingClass = overridden.containingClassLookupTag() ?: return
        reportOn(
            overriding.source,
            chooseCannotChangeAccessPrivilegeFor(overriding),
            overriding.visibility,
            overridden,
            containingClass.name
        )
    }

    context(context: CheckerContext)
    private fun DiagnosticReporter.reportReturnTypeMismatchOnFunction(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>
    ) {
        reportOn(overriding.source, FirErrors.RETURN_TYPE_MISMATCH_ON_OVERRIDE, overriding, overridden)
    }

    context(context: CheckerContext)
    private fun DiagnosticReporter.reportTypeMismatchOnProperty(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>
    ) {
        reportOn(overriding.source, FirErrors.PROPERTY_TYPE_MISMATCH_ON_OVERRIDE, overriding, overridden)
    }

    context(context: CheckerContext)
    private fun DiagnosticReporter.reportTypeMismatchOnVariable(
        overriding: FirCallableSymbol<*>,
        overridden: FirCallableSymbol<*>
    ) {
        reportOn(overriding.source, FirErrors.VAR_TYPE_MISMATCH_ON_OVERRIDE, overriding, overridden)
    }
}
