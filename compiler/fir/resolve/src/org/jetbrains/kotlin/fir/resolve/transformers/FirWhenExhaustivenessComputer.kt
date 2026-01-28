/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.checkUpperBoundViolatedNoReport
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.declaredProperties
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.transformers.WhenOnSealedClassExhaustivenessChecker.ConditionChecker.processBranch
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.computeRepresentativeTypeForBareType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

object FirWhenExhaustivenessComputer {
    private val exhaustivenessCheckers = listOf(
        WhenOnBooleanExhaustivenessChecker,
        WhenOnEnumExhaustivenessChecker,
        WhenOnSealedClassExhaustivenessChecker,
        WhenOnNothingExhaustivenessChecker
    )

    context(_: SessionHolder)
    fun computeAllMissingCases(whenExpression: FirWhenExpression): List<WhenMissingCase> {
        val subjectType = getSubjectType(whenExpression)?.minimumBoundIfFlexible()
            ?: return ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH_REASONS
        return buildList {
            for (type in subjectType.unwrapTypeParameterAndIntersectionTypes()) {
                val checkers = getCheckers(type)
                collectMissingCases(checkers, whenExpression, type)
            }
        }
    }

    context(_: SessionHolder)
    private fun getSubjectType(whenExpression: FirWhenExpression): ConeKotlinType? {
        val subjectType = whenExpression.subjectVariable?.takeUnless {
            it.isImplicitWhenSubjectVariable || (
                    // if the subject variable doesn't have an explicit return type we want to take the
                    // smart-casted type of the initializer instead of original type of the property in the RHS
                    LanguageFeature.ImprovedExhaustivenessCheckForSubjectVariable24.isEnabled() &&
                            (it.returnTypeRef as? FirResolvedTypeRef)?.delegatedTypeRef == null
                    )
        }?.returnTypeRef?.coneType
            ?: whenExpression.subjectVariable?.initializer?.resolvedType
            ?: return null

        return subjectType.fullyExpandedType()
    }

    /**
     * The "minimum" bound of a flexible type is defined as the bound type which will be checked for exhaustion
     * to determine if the when-expression is considered sufficiently exhaustive.
     *
     * * For [dynamic types][ConeDynamicType], this is the **upper bound**,
     * because the branches must cover ***all** possible cases.
     *
     * * For all other [ConeFlexibleType]s, this is the **lower bound**,
     * as platform types may be treated as non-null for exhaustive checks.
     */
    context(_: SessionHolder)
    private fun ConeKotlinType.minimumBoundIfFlexible(): ConeRigidType {
        return when (this) {
            is ConeDynamicType -> when (LanguageFeature.ImprovedExhaustivenessChecksIn21.isEnabled()) {
                true -> upperBound // `dynamic` types must be exhaustive based on the upper bound (`Any?`).
                false -> lowerBound
            }
            is ConeFlexibleType -> lowerBound // All other flexible types may be exhaustive based on the lower bound.
            is ConeRigidType -> this
        }
    }

    context(c: SessionHolder)
    private fun ConeKotlinType.unwrapTypeParameterAndIntersectionTypes(): Collection<ConeKotlinType> {
        return when (this) {
            is ConeIntersectionType -> intersectedTypes
            is ConeTypeParameterType if LanguageFeature.ImprovedExhaustivenessChecksIn21.isEnabled()
                -> buildList {
                lookupTag.typeParameterSymbol.resolvedBounds.flatMapTo(this) {
                    it.coneType.unwrapTypeParameterAndIntersectionTypes()
                }
                add(this@unwrapTypeParameterAndIntersectionTypes)
            }
            is ConeDefinitelyNotNullType if LanguageFeature.ImprovedExhaustivenessChecksIn21.isEnabled()
                -> original.unwrapTypeParameterAndIntersectionTypes()
                .map { it.makeConeTypeDefinitelyNotNullOrNotNull(c.session.typeContext) }
            else -> listOf(this)
        }
    }

    context(_: SessionHolder)
    private fun getCheckers(subjectType: ConeKotlinType): List<WhenExhaustivenessChecker> {
        return buildList {
            exhaustivenessCheckers.filterTo(this) {
                it.isApplicable(subjectType)
            }
            if (isNotEmpty() && subjectType.isMarkedNullable) {
                this.add(WhenOnNullableExhaustivenessChecker)
            }
            if (isEmpty()) {
                // This checker must be the *ONLY* checker when used,
                // as it reports WhenMissingCase.Unknown when it fails.
                add(WhenSelfTypeExhaustivenessChecker)
            }
        }
    }

    context(_: SessionHolder)
    private fun MutableList<WhenMissingCase>.collectMissingCases(
        checkers: List<WhenExhaustivenessChecker>,
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
    ) {
        for (checker in checkers) {
            checker.computeMissingCases(whenExpression, subjectType, this)
        }
        if (isEmpty() && whenExpression.branches.isEmpty()) {
            add(WhenMissingCase.Unknown)
        }
    }

    /**
     * The synthetic call for the whole [whenExpression] might be not completed yet
     */
    context(c: SessionHolder)
    fun computeExhaustivenessStatus(whenExpression: FirWhenExpression, useSiteFile: FirFile): ExhaustivenessStatus {
        return processExhaustivenessCheck(whenExpression).also {
            c.session.enumWhenTracker?.reportEnumUsageInWhen(
                useSiteFile.sourceFile?.path,
                getSubjectType(whenExpression)?.minimumBoundIfFlexible()
            )
        }
    }

    context(_: SessionHolder)
    private fun processExhaustivenessCheck(whenExpression: FirWhenExpression): ExhaustivenessStatus {
        val subjectType = getSubjectType(whenExpression)
        if (subjectType == null) {
            return when {
                whenExpression.hasElseBranch() -> ExhaustivenessStatus.ProperlyExhaustive
                else -> ExhaustivenessStatus.NotExhaustive.noElseBranch(subjectType = null)
            }
        }

        val minimumBound = subjectType.minimumBoundIfFlexible()

        // May not need to calculate the status of the minimum bound if there is an else branch for a platform type subject.
        // In that case, only the upper bound of the platform type needs to be calculated.
        val minimumStatus by lazy(LazyThreadSafetyMode.NONE) { computeExhaustivenessStatus(whenExpression, minimumBound) }

        fun computeUpperBoundStatus(): ExhaustivenessStatus {
            val upperBound = subjectType.upperBoundIfFlexible()
            if (upperBound == minimumBound) return minimumStatus
            return computeExhaustivenessStatus(whenExpression, upperBound)
        }

        val status = when {
            whenExpression.hasElseBranch() -> when {
                // If there is an else branch and the upper-bound is properly exhaustive, the else branch is redundant.
                // Otherwise, the when-expression is properly exhaustive based on the else branch.
                computeUpperBoundStatus() == ExhaustivenessStatus.ProperlyExhaustive -> ExhaustivenessStatus.RedundantlyExhaustive
                else -> ExhaustivenessStatus.ProperlyExhaustive
            }

            else -> minimumStatus
        }

        return status
    }

    private fun FirWhenExpression.hasElseBranch(): Boolean {
        return branches.any { it.condition is FirElseIfTrueCondition }
    }

    context(c: SessionHolder)
    private fun computeExhaustivenessStatus(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
    ): ExhaustivenessStatus {
        val approximatedType = c.session.typeApproximator.approximateToSuperType(
            subjectType, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
        ) ?: subjectType

        if (whenExpression.branches.isEmpty() && approximatedType.isNothing) {
            return ExhaustivenessStatus.ExhaustiveAsNothing
        }

        var status: ExhaustivenessStatus? = null

        val unwrappedIntersectionTypes = approximatedType.unwrapTypeParameterAndIntersectionTypes()

        for (unwrappedSubjectType in unwrappedIntersectionTypes) {
            // `kotlin.Boolean` is always exhaustive despite the fact it could be `expect` (relevant for stdlib K2)
            if (unwrappedSubjectType.toRegularClassSymbol()?.isExpect != true ||
                unwrappedSubjectType.classId == StandardClassIds.Boolean
            ) {
                val localStatus = computeStatusForNonIntersectionType(unwrappedSubjectType, whenExpression)
                when {
                    localStatus === ExhaustivenessStatus.ProperlyExhaustive -> {
                        status = localStatus
                        break
                    }
                    status == null && localStatus != null -> {
                        status = localStatus
                    }
                }
            }
        }

        return status ?: ExhaustivenessStatus.NotExhaustive.noElseBranch(subjectType = approximatedType)
    }

    context(_: SessionHolder)
    private fun computeStatusForNonIntersectionType(
        unwrappedSubjectType: ConeKotlinType,
        whenExpression: FirWhenExpression,
    ): ExhaustivenessStatus? {
        val checkers = getCheckers(unwrappedSubjectType)
        if (checkers.isEmpty()) {
            return null
        }

        val whenMissingCases = mutableListOf<WhenMissingCase>()
        whenMissingCases.collectMissingCases(checkers, whenExpression, unwrappedSubjectType)

        return if (whenMissingCases.isEmpty()) {
            ExhaustivenessStatus.ProperlyExhaustive
        } else {
            ExhaustivenessStatus.NotExhaustive(whenMissingCases, unwrappedSubjectType)
        }
    }
}

private sealed class WhenExhaustivenessChecker {
    context(_: SessionHolder)
    abstract fun isApplicable(subjectType: ConeKotlinType): Boolean

    context(c: SessionHolder)
    abstract fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        destination: MutableCollection<WhenMissingCase>
    )

    protected abstract class AbstractConditionChecker<in D : Any> : FirVisitor<Unit, D>() {
        override fun visitElement(element: FirElement, data: D) {}

        override fun visitWhenExpression(whenExpression: FirWhenExpression, data: D) {
            whenExpression.branches.forEach { it.accept(this, data) }
        }

        override fun visitWhenBranch(whenBranch: FirWhenBranch, data: D) {
            // When conditions with guards do not contribute to exhaustiveness.
            // TODO(KT-63696): enhance exhaustiveness checks to consider guards.
            if (whenBranch.hasGuard) return

            whenBranch.condition.accept(this, data)
        }

        override fun visitBooleanOperatorExpression(booleanOperatorExpression: FirBooleanOperatorExpression, data: D) {
            if (booleanOperatorExpression.kind == LogicOperationKind.OR) {
                booleanOperatorExpression.acceptChildren(this, data)
            }
        }
    }
}

private object WhenOnNullableExhaustivenessChecker : WhenExhaustivenessChecker() {
    context(_: SessionHolder)
    override fun isApplicable(subjectType: ConeKotlinType): Boolean {
        return subjectType.isMarkedOrFlexiblyNullable
    }

    context(c: SessionHolder)
    override fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        destination: MutableCollection<WhenMissingCase>
    ) {
        if (isNullBranchMissing(whenExpression)) {
            destination.add(WhenMissingCase.NullIsMissing)
        }
    }

    fun isNullBranchMissing(whenExpression: FirWhenExpression): Boolean {
        val flags = Flags()
        whenExpression.accept(ConditionChecker, flags)
        return !flags.containsNull
    }

    private class Flags {
        var containsNull = false
    }

    private object ConditionChecker : AbstractConditionChecker<Flags>() {
        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: Flags) {
            val argument = equalityOperatorCall.arguments[1]
            if (argument.resolvedType.isNullableNothing) {
                data.containsNull = true
            }
        }

        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Flags) {
            if (typeOperatorCall.operation == FirOperation.IS && typeOperatorCall.conversionTypeRef.coneType.isMarkedOrFlexiblyNullable) {
                data.containsNull = true
            }
        }
    }
}

private object WhenOnBooleanExhaustivenessChecker : WhenExhaustivenessChecker() {
    context(_: SessionHolder)
    override fun isApplicable(subjectType: ConeKotlinType): Boolean {
        return subjectType.classId == StandardClassIds.Boolean
    }

    private class Flags {
        var containsTrue = false
        var containsFalse = false
    }

    private fun recordValue(value: Any?, data: Flags) = when (value) {
        true -> data.containsTrue = true
        false -> data.containsFalse = true
        else -> {}
    }

    context(c: SessionHolder)
    override fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        destination: MutableCollection<WhenMissingCase>,
    ) {
        if (LanguageFeature.ImprovedExhaustivenessChecksIn21.isEnabled() &&
            WhenSelfTypeExhaustivenessChecker.isExhaustiveThroughSelfTypeCheck(whenExpression, subjectType)
        ) {
            return
        }

        val flags = Flags()
        if (c.session.languageVersionSettings.supportsFeature(LanguageFeature.DataFlowBasedExhaustiveness)) {
            (whenExpression.subjectVariable?.initializer as? FirSmartCastExpression)
                ?.lowerTypesFromSmartCast
                ?.mapNotNull { (it as? DfaType.BooleanLiteral)?.value }
                ?.forEach { recordValue(it, flags) }
        }
        whenExpression.accept(ConditionChecker, flags)
        if (!flags.containsTrue) {
            destination.add(WhenMissingCase.BooleanIsMissing.TrueIsMissing)
        }
        if (!flags.containsFalse) {
            destination.add(WhenMissingCase.BooleanIsMissing.FalseIsMissing)
        }
    }

    private object ConditionChecker : AbstractConditionChecker<Flags>() {
        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: Flags) {
            if (equalityOperatorCall.operation.let { it == FirOperation.EQ || it == FirOperation.IDENTITY }) {
                val argument = equalityOperatorCall.arguments[1]
                if (argument is FirLiteralExpression) {
                    recordValue(argument.value, data)
                }
            }
        }
    }
}

private object WhenOnEnumExhaustivenessChecker : WhenExhaustivenessChecker() {
    context(_: SessionHolder)
    override fun isApplicable(subjectType: ConeKotlinType): Boolean {
        val symbol = subjectType.toRegularClassSymbol() ?: return false
        return symbol.fir.classKind == ClassKind.ENUM_CLASS
    }

    context(c: SessionHolder)
    override fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        destination: MutableCollection<WhenMissingCase>
    ) {
        if (WhenSelfTypeExhaustivenessChecker.isExhaustiveThroughSelfTypeCheck(whenExpression, subjectType)) return

        val enumClass = subjectType.toRegularClassSymbol()!!.fir
        val notCheckedEntries = enumClass.declarations.mapNotNullTo(mutableSetOf()) { it as? FirEnumEntry }

        if (LanguageFeature.DataFlowBasedExhaustiveness.isEnabled()) {
            whenExpression.subjectVariable?.initializer?.let { initializer ->
                val knownNonValues = (initializer as? FirSmartCastExpression)
                    ?.lowerTypesFromSmartCast
                    ?.mapNotNull { (it as? DfaType.Symbol)?.symbol?.fir }
                    .orEmpty()
                notCheckedEntries.removeAll(knownNonValues)
            }
        }

        whenExpression.accept(ConditionChecker, notCheckedEntries)
        notCheckedEntries.mapTo(destination) { WhenMissingCase.EnumCheckIsMissing(it.symbol.callableId) }
    }

    private object ConditionChecker : AbstractConditionChecker<MutableSet<FirEnumEntry>>() {
        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: MutableSet<FirEnumEntry>) {
            if (!equalityOperatorCall.operation.let { it == FirOperation.EQ || it == FirOperation.IDENTITY }) return
            val argument = equalityOperatorCall.arguments[1]

            @OptIn(UnsafeExpressionUtility::class)
            val symbol = argument.toResolvedCallableReferenceUnsafe()?.resolvedSymbol as? FirVariableSymbol<*> ?: return
            val checkedEnumEntry = symbol.fir as? FirEnumEntry ?: return
            data.remove(checkedEnumEntry)
        }
    }
}

private object WhenOnSealedClassExhaustivenessChecker : WhenExhaustivenessChecker() {
    context(_: SessionHolder)
    override fun isApplicable(subjectType: ConeKotlinType): Boolean {
        return subjectType.toRegularClassSymbol()?.fir?.modality == Modality.SEALED
    }

    context(c: SessionHolder)
    override fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        destination: MutableCollection<WhenMissingCase>
    ) {
        val allSubclasses = subjectType.toClassSymbol()?.collectAllSubclasses(c.session) ?: return
        val checkedSubclasses = mutableSetOf<FirClassSymbol<*>>()
        val info = Info(allSubclasses, checkedSubclasses, c.session)

        if (LanguageFeature.DataFlowBasedExhaustiveness.isEnabled()) {
            whenExpression.subjectVariable?.initializer?.let { initializer ->
                inferVariantsFromSubjectSmartCast(initializer, info)
            }
        }
        whenExpression.accept(ConditionChecker, info)
        val notCheckedSubclasses = allSubclasses - checkedSubclasses
        val (notCheckedEnumClasses, notCheckedRegularClasses) = notCheckedSubclasses.partition { it.isEnumClass }

        for (notCheckedEnumClasses in notCheckedEnumClasses) {
            WhenOnEnumExhaustivenessChecker.computeMissingCases(
                whenExpression,
                notCheckedEnumClasses.defaultType(),
                destination
            )
        }

        for (notCheckedRegularClasses in notCheckedRegularClasses) {
            if (!isUninhabited(notCheckedRegularClasses, subjectType, session)) {
                destination += WhenMissingCase.IsTypeCheckIsMissing(
                    notCheckedRegularClasses.classId,
                    notCheckedRegularClasses.fir.classKind.isSingleton,
                    notCheckedRegularClasses.ownTypeParameterSymbols.size
                )
            }
        }
    }

    private fun isUninhabited(
        classSymbol: FirClassSymbol<*>,
        subjectType: ConeKotlinType,
        session: FirSession,
    ): Boolean {
        val classType =
            session.computeRepresentativeTypeForBareType(classSymbol.defaultType(), subjectType) ?: return false
        val boundsViolated =
            checkUpperBoundViolatedNoReport(classSymbol.typeParameterSymbols, classType.typeArguments.toList(), session)
        val containsNothing: Boolean by lazy {
            val typeMapping =
                classSymbol.typeParameterSymbols.zip(classType.typeArguments).mapNotNull { (parameter, arg) ->
                    when (arg) {
                        is ConeKotlinType -> parameter to arg
                        is ConeKotlinTypeProjectionOut -> parameter to arg.type
                        else -> null
                    }
                }.toMap()
            val substitutor = substitutorByMap(typeMapping, session)
            val typesOfProperties = classSymbol.declaredProperties(session)
                .map { substitutor.substituteOrSelf(it.resolvedReturnType) }
            typesOfProperties.any { it.isNothing }
        }
        return boundsViolated || containsNothing
    }

    private fun inferVariantsFromSubjectSmartCast(subject: FirExpression, data: Info) {
        if (subject !is FirSmartCastExpression) return

        for (knownNonType in subject.lowerTypesFromSmartCast) {
            val symbol = when (knownNonType) {
                is DfaType.Cone -> knownNonType.type.toSymbol(data.session)
                is DfaType.Symbol -> knownNonType.symbol
                else -> null
            } as? FirClassSymbol<*> ?: continue
            processBranch(symbol, isNegated = false, data)
        }
    }

    private class Info(
        val allSubclasses: Set<FirClassSymbol<*>>,
        val checkedSubclasses: MutableSet<FirClassSymbol<*>>,
        val session: FirSession
    )

    private object ConditionChecker : AbstractConditionChecker<Info>() {
        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: Info) {
            val isNegated = when (equalityOperatorCall.operation) {
                FirOperation.EQ, FirOperation.IDENTITY -> false
                FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> true
                else -> return
            }
            val argument = equalityOperatorCall.arguments[1].unwrapSmartcastExpression() as? FirResolvedQualifier ?: return
            val firClass = (argument.symbol as? FirRegularClassSymbol)?.fir
            val symbol = if (firClass?.classKind == ClassKind.OBJECT) {
                firClass.symbol
            } else {
                firClass?.companionObjectSymbol
            } ?: return

            processBranch(symbol, isNegated, data)
        }

        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Info) {
            val isNegated = when (typeOperatorCall.operation) {
                FirOperation.IS -> false
                FirOperation.NOT_IS -> true
                else -> return
            }
            val symbol = typeOperatorCall.conversionTypeRef.coneType.fullyExpandedType(data.session).toClassSymbol(data.session) ?: return
            processBranch(symbol, isNegated, data)
        }

        fun processBranch(symbolToCheck: FirClassSymbol<*>, isNegated: Boolean, data: Info) {
            if (data.session.languageVersionSettings.supportsFeature(LanguageFeature.ImprovedExhaustivenessChecksIn23)) {
                processBranchUsingSubtyping(symbolToCheck, isNegated, data)
            } else {
                processBranchUsingSealedInheritors(symbolToCheck, isNegated, data)
            }
        }

        private fun processBranchUsingSubtyping(symbolToCheck: FirClassSymbol<*>, isNegated: Boolean, data: Info) {
            fun FirClassSymbol<*>.isSubclassOf(other: FirClassSymbol<*>): Boolean {
                return this.isSubclassOf(
                    other.toLookupTag(),
                    data.session,
                    isStrict = false,
                    lookupInterfaces = true
                )
            }

            val subclassesCheckedByTheBranch = when (isNegated) {
                // `<subj> is Type`, `<subj> == Type` branches
                false -> data.allSubclasses.filter { subclass ->
                    subclass.isSubclassOf(symbolToCheck)
                }

                // `<subj> !is Type`, `<subj> != Type` branches
                true -> data.allSubclasses.filter { subclass ->
                    !subclass.isSubclassOf(symbolToCheck) && !symbolToCheck.isSubclassOf(subclass)
                }
            }

            // subclassesCheckedByTheBranch
            data.checkedSubclasses.addAll(subclassesCheckedByTheBranch)
        }

        // ----- all functions below should be removed together with the `ImprovedExhaustivenessChecker` language feature -----

        private fun processBranchUsingSealedInheritors(symbolToCheck: FirClassSymbol<*>, isNegated: Boolean, info: Info) {
            val subclassesOfType = symbolToCheck.collectAllSubclasses(info.session)
            val supertypesWhichAreSealedInheritors = symbolToCheck.collectAllSuperclasses(info.session, info)
            if (subclassesOfType.none { it in info.allSubclasses } && supertypesWhichAreSealedInheritors.isEmpty()) {
                return
            }
            val checkedSubclasses = when {
                isNegated -> info.allSubclasses - subclassesOfType
                else -> subclassesOfType + supertypesWhichAreSealedInheritors
            }
            info.checkedSubclasses.addAll(checkedSubclasses)
        }

        private fun FirBasedSymbol<*>.collectAllSuperclasses(session: FirSession, info: Info): Set<FirClassSymbol<*>> {
            if (this !is FirClassSymbol<*>) return emptySet()
            if (this !in info.allSubclasses) return emptySet()
            val lookupTag = this.toLookupTag()
            return info.allSubclasses.filterIsInstance<FirRegularClassSymbol>().filterTo(mutableSetOf()) {
                it.isSubclassOf(lookupTag, session, isStrict = true, lookupInterfaces = true)
            }
        }
    }

    private fun FirClassSymbol<*>.collectAllSubclasses(session: FirSession): Set<FirClassSymbol<*>> {
        return mutableSetOf<FirClassSymbol<*>>().apply { collectAllSubclassesTo(this, session) }
    }

    private fun FirClassSymbol<*>.collectAllSubclassesTo(
        destination: MutableSet<FirClassSymbol<*>>,
        session: FirSession,
        visited: MutableSet<FirRegularClassSymbol> = mutableSetOf(),
    ) {
        if (this !is FirRegularClassSymbol) {
            destination.add(this)
            return
        }
        if (!visited.add(this)) return
        when {
            fir.modality == Modality.SEALED -> {
                if (fir.isJavaNonAbstractSealed == true) {
                    destination.add(this)
                }

                fir.getSealedClassInheritors(session).forEach {
                    val symbol = session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol
                    symbol?.collectAllSubclassesTo(destination, session, visited)
                }
            }
            else -> destination.add(this)
        }
    }
}

private object WhenOnNothingExhaustivenessChecker : WhenExhaustivenessChecker() {
    context(_: SessionHolder)
    override fun isApplicable(subjectType: ConeKotlinType): Boolean {
        return subjectType.isNullableNothing || subjectType.isNothing
    }

    context(c: SessionHolder)
    override fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        destination: MutableCollection<WhenMissingCase>
    ) {
        // Nothing has no branches. The null case for `Nothing?` is handled by WhenOnNullableExhaustivenessChecker
    }
}

/**
 * Checks if any branches are of the same type, or a super-type, of the subject. Must be the only checker when used, as
 * the result of the checker is [WhenMissingCase.Unknown] when no matching branch is found.
 */
private data object WhenSelfTypeExhaustivenessChecker : WhenExhaustivenessChecker() {
    context(_: SessionHolder)
    override fun isApplicable(subjectType: ConeKotlinType): Boolean {
        return true
    }

    context(c: SessionHolder)
    override fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        destination: MutableCollection<WhenMissingCase>,
    ) {
        if (!isExhaustiveThroughSelfTypeCheck(whenExpression, subjectType)) {
            // If there are no cases that check for self-type or super-type, report an Unknown missing case,
            // since we do not want to suggest this sort of check.
            destination.add(WhenMissingCase.Unknown)
        }
    }

    context(c: SessionHolder)
    fun isExhaustiveThroughSelfTypeCheck(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
    ): Boolean {
        /**
         * If the subject type is nullable and one of the branches allows for a nullable type, the subject can be converted to a non-null
         * type, so a non-null self-type case is still considered exhaustive.
         *
         * ```
         * // This is exhaustive!
         * when (x as? String) {
         *     is CharSequence -> ...
         *     null -> ...
         * }
         * ```
         */
        if (WhenOnNullableExhaustivenessChecker.isApplicable(subjectType) &&
            WhenOnNullableExhaustivenessChecker.isNullBranchMissing(whenExpression)
        ) {
            return false
        }

        // If NullIsMissing was *not* reported, the subject can safely be converted to a not-null type.
        val convertedSubjectType = subjectType.withNullability(nullable = false, typeContext = c.session.typeContext)

        val checkedTypes = mutableSetOf<ConeKotlinType>()
        whenExpression.accept(ConditionChecker(c.session), checkedTypes)

        // If there are no cases that check for self-type or super-type, report an Unknown missing case,
        // since we do not want to suggest this sort of check.
        return checkedTypes.any { convertedSubjectType.isSubtypeOf(it, c.session) }
    }

    private class ConditionChecker(val session: FirSession) : AbstractConditionChecker<MutableSet<ConeKotlinType>>() {
        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: MutableSet<ConeKotlinType>) {
            if (typeOperatorCall.operation != FirOperation.IS) return
            data.add(typeOperatorCall.conversionTypeRef.coneType)
        }

        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: MutableSet<ConeKotlinType>) {
            if (!session.languageVersionSettings.supportsFeature(LanguageFeature.DataFlowBasedExhaustiveness)) return
            if (equalityOperatorCall.operation != FirOperation.EQ && equalityOperatorCall.operation != FirOperation.IDENTITY) return
            val argument = equalityOperatorCall.arguments[1]
            val symbol = (argument as? FirResolvedQualifier)?.symbol ?: return

            if (symbol is FirRegularClassSymbol && symbol.classKind == ClassKind.OBJECT) {
                data.add(argument.resolvedType)
            }
        }
    }

}
