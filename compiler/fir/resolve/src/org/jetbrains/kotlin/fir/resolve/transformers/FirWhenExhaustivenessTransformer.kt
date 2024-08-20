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
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

class FirWhenExhaustivenessTransformer(private val bodyResolveComponents: BodyResolveComponents) : FirTransformer<Any?>() {
    companion object {
        private val exhaustivenessCheckers = listOf(
            WhenOnBooleanExhaustivenessChecker,
            WhenOnEnumExhaustivenessChecker,
            WhenOnSealedClassExhaustivenessChecker,
            WhenOnNothingExhaustivenessChecker
        )

        fun computeAllMissingCases(session: FirSession, whenExpression: FirWhenExpression): List<WhenMissingCase> {
            val subjectType =
                getSubjectType(session, whenExpression) ?: return ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH.reasons
            return buildList {
                for (type in subjectType.unwrapTypeParameterAndIntersectionTypes(session)) {
                    val checkers = getCheckers(type, session)
                    collectMissingCases(checkers, whenExpression, type, session)
                }
            }
        }

        private fun getSubjectType(session: FirSession, whenExpression: FirWhenExpression): ConeKotlinType? {
            val subjectType = whenExpression.subjectVariable?.returnTypeRef?.coneType
                ?: whenExpression.subject?.resolvedType
                ?: return null

            return subjectType.fullyExpandedType(session).lowerBoundIfFlexible()
        }

        private fun ConeKotlinType.unwrapTypeParameterAndIntersectionTypes(session: FirSession): Collection<ConeKotlinType> {
            return when {
                this is ConeIntersectionType -> intersectedTypes
                this is ConeTypeParameterType && session.languageVersionSettings.supportsFeature(LanguageFeature.ImprovedExhaustivenessChecksIn21)
                    -> lookupTag.typeParameterSymbol.resolvedBounds.flatMap { it.coneType.unwrapTypeParameterAndIntersectionTypes(session) }
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
            session: FirSession
        ) {
            for (checker in checkers) {
                checker.computeMissingCases(whenExpression, subjectType, session, this)
            }
            if (isEmpty() && whenExpression.branches.isEmpty()) {
                add(WhenMissingCase.Unknown)
            }
        }
    }

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        throw IllegalArgumentException("Should not be there")
    }

    /**
     * The synthetic call for the whole [whenExpression] might be not completed yet
     */
    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: Any?): FirStatement {
        processExhaustivenessCheck(whenExpression)
        bodyResolveComponents.session.enumWhenTracker?.reportEnumUsageInWhen(
            bodyResolveComponents.file.sourceFile?.path,
            getSubjectType(bodyResolveComponents.session, whenExpression)
        )
        return whenExpression
    }

    private fun processExhaustivenessCheck(whenExpression: FirWhenExpression) {
        if (whenExpression.branches.any { it.condition is FirElseIfTrueCondition }) {
            whenExpression.replaceExhaustivenessStatus(ExhaustivenessStatus.ProperlyExhaustive)
            return
        }

        val session = bodyResolveComponents.session
        val subjectType = getSubjectType(session, whenExpression)?.let {
            session.typeApproximator.approximateToSuperType(it, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference) ?: it
        } ?: run {
            whenExpression.replaceExhaustivenessStatus(ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH)
            return
        }

        if (whenExpression.branches.isEmpty() && subjectType.isNothing) {
            whenExpression.replaceExhaustivenessStatus(ExhaustivenessStatus.ExhaustiveAsNothing)
            return
        }

        var status: ExhaustivenessStatus = ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH

        val unwrappedIntersectionTypes = subjectType.unwrapTypeParameterAndIntersectionTypes(bodyResolveComponents.session)

        for (unwrappedSubjectType in unwrappedIntersectionTypes) {
            // `kotlin.Boolean` is always exhaustive despite the fact it could be `expect` (relevant for stdlib K2)
            if (unwrappedSubjectType.toRegularClassSymbol(session)?.isExpect != true ||
                unwrappedSubjectType.classId == StandardClassIds.Boolean
            ) {
                val localStatus = computeStatusForNonIntersectionType(unwrappedSubjectType, session, whenExpression)
                when {
                    localStatus === ExhaustivenessStatus.ProperlyExhaustive -> {
                        status = localStatus
                        break
                    }
                    localStatus !== ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH && status === ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH -> {
                        status = localStatus
                    }
                }
            }
        }

        whenExpression.replaceExhaustivenessStatus(status)
    }

    private fun computeStatusForNonIntersectionType(
        unwrappedSubjectType: ConeKotlinType,
        session: FirSession,
        whenExpression: FirWhenExpression,
    ): ExhaustivenessStatus {
        val checkers = getCheckers(unwrappedSubjectType, session)
        if (checkers.isEmpty()) {
            return ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH
        }

        val whenMissingCases = mutableListOf<WhenMissingCase>()
        whenMissingCases.collectMissingCases(checkers, whenExpression, unwrappedSubjectType, session)

        return if (whenMissingCases.isEmpty()) {
            ExhaustivenessStatus.ProperlyExhaustive
        } else {
            ExhaustivenessStatus.NotExhaustive(whenMissingCases)
        }
    }
}

private sealed class WhenExhaustivenessChecker {
    abstract fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean
    abstract fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        session: FirSession,
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
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        return subjectType.isNullable
    }

    override fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        session: FirSession,
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
            if (typeOperatorCall.conversionTypeRef.coneType.isNullable) {
                data.containsNull = true
            }
        }
    }
}

private object WhenOnBooleanExhaustivenessChecker : WhenExhaustivenessChecker() {
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        return subjectType.classId == StandardClassIds.Boolean
    }

    private class Flags {
        var containsTrue = false
        var containsFalse = false
    }

    override fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>,
    ) {
        if (session.languageVersionSettings.supportsFeature(LanguageFeature.ImprovedExhaustivenessChecksIn21) &&
            WhenSelfTypeExhaustivenessChecker.isExhaustiveThroughSelfTypeCheck(whenExpression, subjectType, session)
        ) {
            return
        }

        val flags = Flags()
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
                    when (argument.value) {
                        true -> data.containsTrue = true
                        false -> data.containsFalse = true
                    }
                }
            }
        }
    }
}

private object WhenOnEnumExhaustivenessChecker : WhenExhaustivenessChecker() {
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        val symbol = subjectType.toRegularClassSymbol(session) ?: return false
        return symbol.fir.classKind == ClassKind.ENUM_CLASS
    }

    override fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>
    ) {
        if (WhenSelfTypeExhaustivenessChecker.isExhaustiveThroughSelfTypeCheck(whenExpression, subjectType, session)) return

        val enumClass = (subjectType.toSymbol(session) as FirRegularClassSymbol).fir
        val notCheckedEntries = enumClass.declarations.mapNotNullTo(mutableSetOf()) { it as? FirEnumEntry }
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
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        return subjectType.toRegularClassSymbol(session)?.fir?.modality == Modality.SEALED
    }

    override fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>
    ) {
        val allSubclasses = subjectType.toSymbol(session)?.collectAllSubclasses(session) ?: return
        val checkedSubclasses = mutableSetOf<FirBasedSymbol<*>>()
        whenExpression.accept(ConditionChecker, Flags(allSubclasses, checkedSubclasses, session))
        (allSubclasses - checkedSubclasses).mapNotNullTo(destination) {
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

    private class Flags(
        val allSubclasses: Set<FirBasedSymbol<*>>,
        val checkedSubclasses: MutableSet<FirBasedSymbol<*>>,
        val session: FirSession
    )

    private object ConditionChecker : AbstractConditionChecker<Flags>() {
        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: Flags) {
            val isNegated = when (equalityOperatorCall.operation) {
                FirOperation.EQ, FirOperation.IDENTITY -> false
                FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> true
                else -> return
            }

            val symbol = when (val argument = equalityOperatorCall.arguments[1].unwrapSmartcastExpression()) {
                is FirResolvedQualifier -> {
                    val firClass = (argument.symbol as? FirRegularClassSymbol)?.fir
                    if (firClass?.classKind == ClassKind.OBJECT) {
                        firClass.symbol
                    } else {
                        firClass?.companionObjectSymbol
                    }
                }
                else -> {
                    @OptIn(UnsafeExpressionUtility::class)
                    argument.toResolvedCallableSymbolUnsafe()?.takeIf { it.fir is FirEnumEntry }
                }
            } ?: return
            processBranch(symbol, isNegated, data)
        }

        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Flags) {
            val isNegated = when (typeOperatorCall.operation) {
                FirOperation.IS -> false
                FirOperation.NOT_IS -> true
                else -> return
            }
            val symbol = typeOperatorCall.conversionTypeRef.coneType.fullyExpandedType(data.session).toSymbol(data.session) ?: return
            processBranch(symbol, isNegated, data)
        }

        private fun processBranch(symbolToCheck: FirBasedSymbol<*>, isNegated: Boolean, flags: Flags) {
            val subclassesOfType = symbolToCheck.collectAllSubclasses(flags.session)
            if (subclassesOfType.none { it in flags.allSubclasses }) {
                return
            }
            val checkedSubclasses = if (isNegated) flags.allSubclasses - subclassesOfType else subclassesOfType
            flags.checkedSubclasses.addAll(checkedSubclasses)
        }
    }


    private fun FirBasedSymbol<*>.collectAllSubclasses(session: FirSession): Set<FirBasedSymbol<*>> {
        return mutableSetOf<FirBasedSymbol<*>>().apply { collectAllSubclassesTo(this, session) }
    }

    private fun FirBasedSymbol<*>.collectAllSubclassesTo(destination: MutableSet<FirBasedSymbol<*>>, session: FirSession) {
        if (this !is FirRegularClassSymbol) {
            destination.add(this)
            return
        }
        when {
            fir.modality == Modality.SEALED -> fir.getSealedClassInheritors(session).forEach {
                val symbol = session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol
                symbol?.collectAllSubclassesTo(destination, session)
            }
            fir.classKind == ClassKind.ENUM_CLASS -> fir.collectEnumEntries().mapTo(destination) { it.symbol }
            else -> destination.add(this)
        }
    }
}

private object WhenOnNothingExhaustivenessChecker : WhenExhaustivenessChecker() {
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        return subjectType.isNullableNothing || subjectType.isNothing
    }

    override fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
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
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>,
    ) {
        // This checker should only be used when no other missing cases are being reported.
        if (destination.isNotEmpty()) return

        if (!isExhaustiveThroughSelfTypeCheck(whenExpression, subjectType, session)) {
            // If there are no cases that check for self-type or super-type, report an Unknown missing case,
            // since we do not want to suggest this sort of check.
            destination.add(WhenMissingCase.Unknown)
        }
    }

    fun isExhaustiveThroughSelfTypeCheck(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
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
            WhenOnNullableExhaustivenessChecker.isNullBranchMissing(whenExpression)
        ) {
            return false
        }

        // If NullIsMissing was *not* reported, the subject can safely be converted to a not-null type.
        val convertedSubjectType = subjectType.withNullability(nullability = ConeNullability.NOT_NULL, typeContext = session.typeContext)

        val checkedTypes = mutableSetOf<ConeKotlinType>()
        whenExpression.accept(ConditionChecker, checkedTypes)

        // If there are no cases that check for self-type or super-type, report an Unknown missing case,
        // since we do not want to suggest this sort of check.
        return checkedTypes.any { convertedSubjectType.isSubtypeOf(it, session) }
    }

    private object ConditionChecker : AbstractConditionChecker<MutableSet<ConeKotlinType>>() {
        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: MutableSet<ConeKotlinType>) {
            if (typeOperatorCall.operation != FirOperation.IS) return
            data.add(typeOperatorCall.conversionTypeRef.coneType)
        }
    }

}
