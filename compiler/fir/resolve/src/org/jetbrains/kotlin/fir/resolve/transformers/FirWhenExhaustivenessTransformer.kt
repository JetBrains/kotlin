/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.enumWhenTracker
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.reportEnumUsageInWhen
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.StandardClassIds

class FirWhenExhaustivenessTransformer(private val bodyResolveComponents: BodyResolveComponents) : FirTransformer<Any?>() {
    companion object {
        private val exhaustivenessCheckers = listOf(
            WhenOnBooleanExhaustivenessChecker,
            WhenOnEnumExhaustivenessChecker,
            WhenOnSealedClassExhaustivenessChecker,
            WhenOnNothingExhaustivenessChecker
        )

        fun computeAllMissingCases(session: FirSession, whenExpression: FirWhenExpression): List<WhenMissingCase> {
            val subjectType = getSubjectType(session, whenExpression) ?: return emptyList()
            return buildList {
                for (type in subjectType.unwrapIntersectionType()) {
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

        private fun ConeKotlinType.unwrapIntersectionType(): Collection<ConeKotlinType> {
            return (this as? ConeIntersectionType)?.intersectedTypes ?: listOf(this)
        }


        private fun getCheckers(
            subjectType: ConeKotlinType,
            session: FirSession
        ): List<WhenExhaustivenessChecker> {
            return buildList {
                exhaustivenessCheckers.filterTo<WhenExhaustivenessChecker, MutableCollection<in WhenExhaustivenessChecker>>(this) {
                    it.isApplicable(subjectType, session)
                }
                if (isNotEmpty() && subjectType.isMarkedNullable) {
                    this.add(WhenOnNullableExhaustivenessChecker)
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
        val subjectType = getSubjectType(session, whenExpression) ?: run {
            whenExpression.replaceExhaustivenessStatus(ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH)
            return
        }

        if (whenExpression.branches.isEmpty() && subjectType.isNothing) {
            whenExpression.replaceExhaustivenessStatus(ExhaustivenessStatus.ExhaustiveAsNothing)
            return
        }

        var status: ExhaustivenessStatus = ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH

        if (subjectType.toRegularClassSymbol(session)?.isExpect != true) {
            val unwrappedIntersectionTypes = subjectType.unwrapIntersectionType()

            for (unwrappedSubjectType in unwrappedIntersectionTypes) {
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
            whenBranch.condition.accept(this, data)
        }

        override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: D) {
            if (binaryLogicExpression.kind == LogicOperationKind.OR) {
                binaryLogicExpression.acceptChildren(this, data)
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
        val flags = Flags()
        whenExpression.accept(ConditionChecker, flags)
        if (!flags.containsNull) {
            destination.add(WhenMissingCase.NullIsMissing)
        }
    }

    private class Flags {
        var containsNull = false
    }

    private object ConditionChecker : AbstractConditionChecker<Flags>() {
        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: Flags) {
            val argument = equalityOperatorCall.arguments[1]
            if (argument.coneTypeOrNull?.isNullableNothing == true) {
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
        destination: MutableCollection<WhenMissingCase>
    ) {
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
                if (argument is FirConstExpression<*>) {
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
        val symbol = subjectType.toSymbol(session) as? FirRegularClassSymbol ?: return false
        return symbol.fir.classKind == ClassKind.ENUM_CLASS
    }

    override fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>
    ) {
        val enumClass = (subjectType.toSymbol(session) as FirRegularClassSymbol).fir
        val allEntries = enumClass.declarations.mapNotNullTo(mutableSetOf()) { it as? FirEnumEntry }
        val checkedEntries = mutableSetOf<FirEnumEntry>()
        whenExpression.accept(ConditionChecker, checkedEntries)
        val notCheckedEntries = allEntries - checkedEntries
        notCheckedEntries.mapTo(destination) { WhenMissingCase.EnumCheckIsMissing(it.symbol.callableId) }
    }

    private object ConditionChecker : AbstractConditionChecker<MutableSet<FirEnumEntry>>() {
        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: MutableSet<FirEnumEntry>) {
            if (!equalityOperatorCall.operation.let { it == FirOperation.EQ || it == FirOperation.IDENTITY }) return
            val argument = equalityOperatorCall.arguments[1]
            val symbol = argument.toResolvedCallableReference()?.resolvedSymbol as? FirVariableSymbol<*> ?: return
            val checkedEnumEntry = symbol.fir as? FirEnumEntry ?: return
            data.add(checkedEnumEntry)
        }
    }
}

private object WhenOnSealedClassExhaustivenessChecker : WhenExhaustivenessChecker() {
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        return (subjectType.toSymbol(session)?.fir as? FirRegularClass)?.modality == Modality.SEALED
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
                is FirClassSymbol<*> -> WhenMissingCase.IsTypeCheckIsMissing(it.classId, it.fir.classKind.isSingleton)
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

            val symbol = when (val argument = equalityOperatorCall.arguments[1]) {
                is FirResolvedQualifier -> {
                    val firClass = (argument.symbol as? FirRegularClassSymbol)?.fir
                    if (firClass?.classKind == ClassKind.OBJECT) {
                        firClass.symbol
                    } else {
                        firClass?.companionObjectSymbol
                    }
                }
                else -> {
                    argument.toResolvedCallableSymbol()?.takeIf { it.fir is FirEnumEntry }
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
