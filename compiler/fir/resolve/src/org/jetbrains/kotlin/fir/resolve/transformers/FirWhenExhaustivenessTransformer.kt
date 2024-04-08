/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.ExhaustivenessStatus
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.dfa.BranchStatement
import org.jetbrains.kotlin.fir.resolve.dfa.RealVariable
import org.jetbrains.kotlin.fir.resolve.dfa.TypeStatement
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraphNode
import org.jetbrains.kotlin.fir.resolve.dfa.isVacuousIntersection
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

class FirWhenExhaustivenessTransformer(private val bodyResolveComponents: BodyResolveComponents) : FirTransformer<TypeStatement?>() {
    companion object {
        private val exhaustivenessCheckers = listOf(
            WhenOnBooleanExhaustivenessChecker,
            WhenOnEnumExhaustivenessChecker,
            WhenOnSealedClassExhaustivenessChecker,
            WhenOnNothingExhaustivenessChecker
        )

        fun computeAllMissingCases(session: FirSession, whenExpression: FirWhenExpression): List<WhenMissingCase> {
            val elseNode = whenExpression.elseControlFlowGraphNodeReference?.controlFlowGraphNode
                ?: return emptyList() // if there is no synthetic (or specified) 'else', that means it was correct
            val types = whenExpression.dataFlowVariable?.let { elseNode.flow.getTypeStatement(elseNode.flow.unwrapVariable(it)) }
            return computeAllMissingCases(session, whenExpression, types)
        }

        fun computeAllMissingCases(session: FirSession, whenExpression: FirWhenExpression, types: TypeStatement?): List<WhenMissingCase> {
            val subjectType = getSubjectType(session, whenExpression, types)?.minimumBoundIfFlexible(session)
                ?: return ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH_REASONS
            return buildList {
                for (type in subjectType.unwrapTypeParameterAndIntersectionTypes(session)) {
                    val checkers = getCheckers(type, session)
                    collectMissingCases(checkers, whenExpression, type, types, session)
                }
            }
        }

        private fun getSubjectType(session: FirSession, whenExpression: FirWhenExpression, types: TypeStatement?): ConeKotlinType? {
            val subjectType = whenExpression.subjectVariable?.returnTypeRef?.coneType
                ?: whenExpression.subject?.resolvedType
                ?: return null

            return ConeTypeIntersector
                .intersectTypes(session.typeContext, listOf(subjectType) + types?.exactType.orEmpty())
                .fullyExpandedType(session)
        }

        private val FirWhenExpression.dataFlowVariable: RealVariable?
            get() = subjectVariable?.let { RealVariable.local(it.symbol) } ?: subject?.let { RealVariable.whenSubject(this) }

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
        private fun ConeKotlinType.minimumBoundIfFlexible(session: FirSession): ConeRigidType {
            return when (this) {
                is ConeDynamicType -> when (session.languageVersionSettings.supportsFeature(LanguageFeature.ImprovedExhaustivenessChecksIn21)) {
                    true -> upperBound // `dynamic` types must be exhaustive based on the upper bound (`Any?`).
                    false -> lowerBound
                }
                is ConeFlexibleType -> lowerBound // All other flexible types may be exhaustive based on the lower bound.
                is ConeRigidType -> this
            }
        }

        private fun ConeKotlinType.unwrapTypeParameterAndIntersectionTypes(session: FirSession): Collection<ConeKotlinType> {
            return when {
                this is ConeIntersectionType -> intersectedTypes
                this is ConeTypeParameterType && session.languageVersionSettings.supportsFeature(LanguageFeature.ImprovedExhaustivenessChecksIn21)
                    -> buildList {
                    lookupTag.typeParameterSymbol.resolvedBounds.flatMapTo(this) {
                        it.coneType.unwrapTypeParameterAndIntersectionTypes(session)
                    }
                    add(this@unwrapTypeParameterAndIntersectionTypes)
                }
                this is ConeDefinitelyNotNullType && session.languageVersionSettings.supportsFeature(LanguageFeature.ImprovedExhaustivenessChecksIn21)
                    -> original.unwrapTypeParameterAndIntersectionTypes(session)
                    .map { it.makeConeTypeDefinitelyNotNullOrNotNull(session.typeContext) }
                else -> listOf(this)
            }
        }

        private fun getCheckers(
            subjectType: ConeKotlinType,
            session: FirSession
        ): List<WhenExhaustivenessChecker> {
            return buildList<WhenExhaustivenessChecker> {
                exhaustivenessCheckers.filterTo(this) {
                    it.isApplicable(subjectType, session)
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

        private fun MutableList<WhenMissingCase>.collectMissingCases(
            checkers: List<WhenExhaustivenessChecker>,
            whenExpression: FirWhenExpression,
            subjectType: ConeKotlinType,
            types: TypeStatement?,
            session: FirSession
        ) {
            for (checker in checkers) {
                checker.computeMissingCases(subjectType, types, session, this)
            }
            if (isEmpty() && whenExpression.branches.isEmpty()) {
                add(WhenMissingCase.Unknown)
            }
        }
    }

    override fun <E : FirElement> transformElement(element: E, data: TypeStatement?): E {
        throw IllegalArgumentException("Should not be there")
    }

    /**
     * The synthetic call for the whole [whenExpression] might be not completed yet
     */
    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: TypeStatement?): FirStatement {
        processExhaustivenessCheck(whenExpression, data)
        bodyResolveComponents.session.enumWhenTracker?.reportEnumUsageInWhen(
            bodyResolveComponents.file.sourceFile?.path,
            getSubjectType(bodyResolveComponents.session, whenExpression, data)?.minimumBoundIfFlexible(bodyResolveComponents.session)
        )
        return whenExpression
    }

    private fun processExhaustivenessCheck(whenExpression: FirWhenExpression, types: TypeStatement?) {
        val session = bodyResolveComponents.session
        val subjectType = getSubjectType(session, whenExpression, types)
        if (subjectType == null) {
            whenExpression.replaceExhaustivenessStatus(
                when {
                    whenExpression.hasElseBranch() -> ExhaustivenessStatus.ProperlyExhaustive()
                    else -> ExhaustivenessStatus.NotExhaustive.noElseBranch(subjectType = null)
                }
            )
            return
        }

        val minimumBound = subjectType.minimumBoundIfFlexible(session)

        // May not need to calculate the status of the minimum bound if there is an else branch for a platform type subject.
        // In that case, only the upper bound of the platform type needs to be calculated.
        val minimumStatus by lazy { computeExhaustivenessStatus(whenExpression, minimumBound, types) }

        fun computeUpperBoundStatus(): ExhaustivenessStatus {
            val upperBound = subjectType.upperBoundIfFlexible()
            if (upperBound == minimumBound) return minimumStatus
            return computeExhaustivenessStatus(whenExpression, upperBound, types)
        }

        val status = when {
            whenExpression.hasElseBranch() -> {
                when (val upperBoundStatus = computeUpperBoundStatus()) {
                    // If there is an else branch and the upper-bound is properly exhaustive, the else branch is redundant.
                    // Otherwise, the when-expression is properly exhaustive based on the else branch.
                    is ExhaustivenessStatus.ProperlyExhaustive -> ExhaustivenessStatus.RedundantlyExhaustive(
                        unusedOrSafeNegativeInformation = upperBoundStatus.unusedOrSafeNegativeInformation
                    )
                    else -> ExhaustivenessStatus.ProperlyExhaustive()
                }
            }

            else -> minimumStatus
        }

        whenExpression.replaceExhaustivenessStatus(status)
    }

    private fun FirWhenExpression.hasElseBranch(): Boolean {
        return branches.any { it.condition is FirElseIfTrueCondition }
    }

    private fun computeExhaustivenessStatus(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        types: TypeStatement?
    ): ExhaustivenessStatus {
        val session = bodyResolveComponents.session
        val approximatedType = session.typeApproximator.approximateToSuperType(
            subjectType, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
        ) ?: subjectType

        if (whenExpression.branches.isEmpty() && approximatedType.isNothing) {
            return ExhaustivenessStatus.ExhaustiveAsNothing
        }

        var status: ExhaustivenessStatus? = null

        val unwrappedIntersectionTypes = approximatedType.unwrapTypeParameterAndIntersectionTypes(session)
        if (unwrappedIntersectionTypes.isVacuousIntersection(session)) {
            return ExhaustivenessStatus.ProperlyExhaustive()
        }

        for (unwrappedSubjectType in unwrappedIntersectionTypes) {
            // `kotlin.Boolean` is always exhaustive despite the fact it could be `expect` (relevant for stdlib K2)
            if (unwrappedSubjectType.toRegularClassSymbol(session)?.isExpect != true ||
                unwrappedSubjectType.classId == StandardClassIds.Boolean
            ) {
                val localStatus = computeStatusForNonIntersectionType(unwrappedSubjectType, types, session, whenExpression)
                when {
                    localStatus is ExhaustivenessStatus.ProperlyExhaustive -> {
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

    private fun computeStatusForNonIntersectionType(
        unwrappedSubjectType: ConeKotlinType,
        types: TypeStatement?,
        session: FirSession,
        whenExpression: FirWhenExpression,
    ): ExhaustivenessStatus? {
        val checkers = getCheckers(unwrappedSubjectType, session)
        if (checkers.isEmpty()) {
            return null
        }

        val whenMissingCases = mutableListOf<WhenMissingCase>()
        whenMissingCases.collectMissingCases(checkers, whenExpression, unwrappedSubjectType, types, session)

        return when {
            whenMissingCases.isEmpty() -> ExhaustivenessStatus.ProperlyExhaustive(
                unusedOrSafeNegativeInformation = types?.safeNegativeInformation ?: true
            )
            else -> ExhaustivenessStatus.NotExhaustive(whenMissingCases, unwrappedSubjectType)
        }
    }
}

private sealed class WhenExhaustivenessChecker {
    abstract fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean

    abstract fun computeMissingCases(
        subjectType: ConeKotlinType,
        types: TypeStatement?,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>
    )
}

private object WhenOnNullableExhaustivenessChecker : WhenExhaustivenessChecker() {
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        return subjectType.isMarkedOrFlexiblyNullable
    }

    override fun computeMissingCases(
        subjectType: ConeKotlinType,
        types: TypeStatement?,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>
    ) {
        if (isNullBranchMissing(types)) {
            destination.add(WhenMissingCase.NullIsMissing)
        }
    }

    fun isNullBranchMissing(types: TypeStatement?): Boolean =
        types == null || types.negativeInformation.none { it is BranchStatement.Is && it.type.isMarkedNullable }
}

private object WhenOnBooleanExhaustivenessChecker : WhenExhaustivenessChecker() {
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        return subjectType.classId == StandardClassIds.Boolean
    }

    override fun computeMissingCases(
        subjectType: ConeKotlinType,
        types: TypeStatement?,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>,
    ) {
        if (session.languageVersionSettings.supportsFeature(LanguageFeature.ImprovedExhaustivenessChecksIn21) &&
            WhenSelfTypeExhaustivenessChecker.isExhaustiveThroughSelfTypeCheck(subjectType, types, session)
        ) {
            return
        }

        val values = types?.negativeInformation?.filterIsInstance<BranchStatement.BooleanValue>()?.map { it.value }.orEmpty()
        if (true !in values) destination.add(WhenMissingCase.BooleanIsMissing.TrueIsMissing)
        if (false !in values) destination.add(WhenMissingCase.BooleanIsMissing.FalseIsMissing)
    }
}

private object WhenOnEnumExhaustivenessChecker : WhenExhaustivenessChecker() {
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        val symbol = subjectType.toRegularClassSymbol(session) ?: return false
        return symbol.fir.classKind == ClassKind.ENUM_CLASS
    }

    override fun computeMissingCases(
        subjectType: ConeKotlinType,
        types: TypeStatement?,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>
    ) {
        if (WhenSelfTypeExhaustivenessChecker.isExhaustiveThroughSelfTypeCheck(subjectType, types, session)) return

        val enumClass = (subjectType.toSymbol(session) as FirRegularClassSymbol).fir

        val remainingEntries = enumClass.declarations.mapNotNull { it as? FirEnumEntry }.toMutableSet()
        if (types != null) {
            remainingEntries.removeAll(types.negativeEnumEntries().map { it.fir })
        }
        remainingEntries.mapTo(destination) { WhenMissingCase.EnumCheckIsMissing(it.symbol.callableId) }
    }
}

private object WhenOnSealedClassExhaustivenessChecker : WhenExhaustivenessChecker() {
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        return subjectType.toRegularClassSymbol(session)?.fir?.modality == Modality.SEALED
    }

    override fun computeMissingCases(
        subjectType: ConeKotlinType,
        types: TypeStatement?,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>
    ) {
        val remainingSubclasses = subjectType.toSymbol(session)?.collectAllSubclasses(session)?.toMutableSet() ?: return
        types?.negativeSymbols(session)?.forEach { negativeSubclass ->
            remainingSubclasses.removeAll(negativeSubclass.collectAllSubclasses(session))
        }
        remainingSubclasses.mapNotNullTo(destination) {
            when (it) {
                is FirClassSymbol<*> -> WhenMissingCase.IsTypeCheckIsMissing(
                    it.classId,
                    it.fir.classKind.isSingleton,
                    it.ownTypeParameterSymbols.size
                )
                is FirVariableSymbol<*> -> WhenMissingCase.EnumCheckIsMissing(it.callableId)
                else -> null
            }
        }
    }
}

private object WhenOnNothingExhaustivenessChecker : WhenExhaustivenessChecker() {
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        return subjectType.isNullableNothing || subjectType.isNothing
    }

    override fun computeMissingCases(
        subjectType: ConeKotlinType,
        types: TypeStatement?,
        session: FirSession,
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
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        return true
    }

    override fun computeMissingCases(
        subjectType: ConeKotlinType,
        types: TypeStatement?,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>,
    ) {
        // This checker should only be used when no other missing cases are being reported.
        if (destination.isNotEmpty()) return

        if (!isExhaustiveThroughSelfTypeCheck(subjectType, types, session)) {
            // If there are no cases that check for self-type or super-type, report an Unknown missing case,
            // since we do not want to suggest this sort of check.
            destination.add(WhenMissingCase.Unknown)
        }
    }

    fun isExhaustiveThroughSelfTypeCheck(
        subjectType: ConeKotlinType,
        types: TypeStatement?,
        session: FirSession,
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
        if (WhenOnNullableExhaustivenessChecker.isApplicable(subjectType, session) &&
            WhenOnNullableExhaustivenessChecker.isNullBranchMissing(types)
        ) {
            return false
        }

        // If NullIsMissing was *not* reported, the subject can safely be converted to a not-null type.
        val convertedSubjectType = subjectType.withNullability(nullable = false, typeContext = session.typeContext)

        // If there are no cases that check for self-type or super-type, report an Unknown missing case,
        // since we do not want to suggest this sort of check.
        return types?.negativeTypes()?.any { convertedSubjectType.isSubtypeOf(it, session) } == true
    }

}

private fun TypeStatement.negativeSymbols(session: FirSession) =
    negativeTypes().mapNotNull { it.toSymbol(session) } + negativeEnumEntries()

private fun TypeStatement.negativeTypes() =
    negativeInformation.filterIsInstance<BranchStatement.Is>().map { it.type }

private fun TypeStatement.negativeEnumEntries() =
    negativeInformation.filterIsInstance<BranchStatement.EnumEntry>().map { it.entry }
