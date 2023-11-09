/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.diagnostics.WhenMissingCaseFor
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.dfa.PropertyStability
import org.jetbrains.kotlin.fir.resolve.dfa.RealVariable
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions

class FirWhenExhaustivenessTransformer2(private val bodyResolveComponents: BodyResolveComponents) : FirTransformer<Any?>() {
    private val session: FirSession = bodyResolveComponents.session
    private val dataFlowAnalyzer: FirDataFlowAnalyzer = bodyResolveComponents.dataFlowAnalyzer

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        throw IllegalArgumentException("Should not be there")
    }

    /**
     * The synthetic call for the whole [whenExpression] might be not completed yet
     */
    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: Any?): FirStatement {
        processExhaustivenessCheck(whenExpression)
        session.enumWhenTracker?.reportEnumUsageInWhen(
            bodyResolveComponents.file.sourceFile?.path,
            FirWhenExhaustivenessTransformer.getSubjectType(session, whenExpression)
        )
        return whenExpression
    }

    private fun processExhaustivenessCheck(whenExpression: FirWhenExpression) {
        if (whenExpression.branches.any { it.condition is FirElseIfTrueCondition } || whenExpression.isTrueAndFalse()) {
            whenExpression.replaceExhaustivenessStatus(ExhaustivenessStatus.ProperlyExhaustive)
            return
        }

        // compute the information about variables and expressions
        val realVariables = mutableMapOf<VariableSource, VariableInformation>()
        val subject = whenExpression.subject
        if (subject != null) {
            val subjectType = FirWhenExhaustivenessTransformer.getSubjectType(session, whenExpression)
            val subjectTypeIsExpect = subjectType?.toRegularClassSymbol(session)?.isExpect
            // bail out early if we don't know the type of the subject, or it's expect
            if (subjectType == null || subjectTypeIsExpect == true) {
                whenExpression.replaceExhaustivenessStatus(ExhaustivenessStatus.NotExhaustive.unknownMissingCase(subject.source))
                return
            }
            if (subjectType.isNothing) {
                whenExpression.replaceExhaustivenessStatus(ExhaustivenessStatus.ExhaustiveAsNothing)
                return
            }
            // add the subject information
            val subjectVariable = dataFlowAnalyzer.getVariableFromSmartcastInfo(subject)
            val subjectSource = subjectVariable?.let { VariableSource.Real(it) } ?: VariableSource.Subject
            realVariables[subjectSource] = VariableInformation(subject, subjectType)
        }
        val stableExpressions = whenExpression.branches.flatMap { computeStableExpressions(whenExpression, it.condition, realVariables) }

        if (realVariables.isEmpty() || stableExpressions.isEmpty()) {
            whenExpression.replaceExhaustivenessStatus(ExhaustivenessStatus.NotExhaustive.unknownMissingCase(null))
            return
        }

        val problems: List<List<Pair<VariableSource, WhenChoice>>> = stableExpressions.checkExhaustiveness(realVariables.toList())
        val missingCases = problems.map { problem ->
            problem.map { (variable, choice) ->
                val originalExpression = realVariables[variable]?.originalExpression
                val reportedExpression = originalExpression.takeIf { variable != VariableSource.Subject }?.source
                WhenMissingCaseFor(reportedExpression, choice.toWhenMissingCase())
            }
        }
        if (missingCases.isEmpty()) {
            whenExpression.replaceExhaustivenessStatus(ExhaustivenessStatus.ProperlyExhaustive)
        } else {
            whenExpression.replaceExhaustivenessStatus(ExhaustivenessStatus.NotExhaustive(missingCases))
        }
    }

    private val notUsefulBranch: List<WhenBranchList> = emptyList()

    // this function performs a few things at the same time:
    // - adds information about the types
    // - flattens nested 'or' and 'and'
    // - returns 'null' if a non-stable variable or an unknown expression is mentioned
    private fun computeStableExpressions(
        completeWhenExpression: FirWhenExpression,
        expression: FirExpression,
        result: MutableMap<VariableSource, VariableInformation>,
    ): List<WhenBranchList> {
        when (expression) {
            is FirElseIfTrueCondition -> return listOf(WhenBranchList.empty())
            is FirBinaryLogicExpression -> {
                val left = computeStableExpressions(completeWhenExpression, expression.leftOperand, result)
                val right = computeStableExpressions(completeWhenExpression, expression.rightOperand, result)
                return when (expression.kind) {
                    LogicOperationKind.OR -> left + right
                    LogicOperationKind.AND -> return left.flatMap { l -> right.map { r -> l + r } }
                }
            }
            is FirEqualityOperatorCall, is FirTypeOperatorCall -> {
                expression as FirCall
                return getInformation(completeWhenExpression, expression.arguments[0], expression, result)?.let { listOf(it) }
                    ?: notUsefulBranch
            }
            is FirFunctionCall -> {
                val operator = expression.toResolvedCallableSymbol() ?: return notUsefulBranch
                if (!operator.isOperator || operator.name != OperatorNameConventions.NOT) return notUsefulBranch
                val receiver = expression.dispatchReceiver ?: return notUsefulBranch
                return getInformation(completeWhenExpression, receiver, expression, result)?.let { listOf(it) } ?: notUsefulBranch
            }
            else -> {
                // lone Boolean variables
                return getInformation(completeWhenExpression, expression, expression, result)?.let { listOf(it) } ?: notUsefulBranch
            }
        }
    }

    private fun getInformation(
        completeWhenExpression: FirWhenExpression,
        variable: FirExpression,
        whole: FirExpression,
        result: MutableMap<VariableSource, VariableInformation>,
    ): WhenBranchList? {
        val realVar = dataFlowAnalyzer.getVariableFromSmartcastInfo(variable)
        // case in which we have the subject variable
        if (variable is FirWhenSubjectExpression && variable.whenRef.value === completeWhenExpression) {
            val subjectSource = realVar?.let { VariableSource.Real(it) } ?: VariableSource.Subject
            return WhenBranchList.from(subjectSource, whole)
        }
        // otherwise we should have a stable reference
        if (realVar?.stability != PropertyStability.STABLE_VALUE) return null
        val source = VariableSource.Real(realVar)
        if (source !in result) { // add information the first time
            result[VariableSource.Real(realVar)] = VariableInformation.fromExpression(variable, session)
        }
        return WhenBranchList.from(source, whole)
    }

    private fun List<WhenBranchList>.checkExhaustiveness(
        variables: List<Pair<VariableSource, VariableInformation>>,
    ): List<MutableList<Pair<VariableSource, WhenChoice>>> {
        if (variables.isEmpty()) return mutableListOf()
        val (currentVariable, currentInfo) = variables.first()
        val restOfVariables = variables.drop(1)
        // if it's not mentioned, let's not even consider that one
        if (none { it.mentions(currentVariable) }) {
            return checkExhaustiveness(restOfVariables)
        }
        // now let's build the problems by checking each possible choice
        val exhaustivenessProblems: MutableList<MutableList<Pair<VariableSource, WhenChoice>>> = mutableListOf()
        for (currentChoice in currentInfo.type.choices(session)) {
            val elements = filter { branch ->
                branch[currentVariable]?.all { currentChoice.coveredBy(session, it) } ?: true
            }
            when {
                elements.isEmpty() -> exhaustivenessProblems.add(mutableListOf(currentVariable to currentChoice))
                else -> elements.checkExhaustiveness(restOfVariables).forEach { problem ->
                    // take all the problems we found, and add the current choice at the front
                    problem.add(0, currentVariable to currentChoice)
                    exhaustivenessProblems.add(problem)
                }
            }
        }
        return exhaustivenessProblems
    }

    // special case of when { true -> ..., false -> ... }
    @Suppress("UNCHECKED_CAST")
    private fun FirWhenExpression.isTrueAndFalse(): Boolean {
        if (subject != null) return false
        val covered = mutableListOf<Boolean>()
        for (branch in branches) {
            val condition = branch.condition
            if (condition !is FirConstExpression<*>) continue
            if (condition.kind != ConstantValueKind.Boolean) continue
            covered.add((condition as FirConstExpression<Boolean>).value)
        }
        return true in covered && false in covered
    }
}

sealed interface VariableSource {
    data object Subject : VariableSource
    data class Real(val variable: RealVariable) : VariableSource
}

data class VariableInformation(
    val originalExpression: FirExpression,
    val type: ConeKotlinType,
) {
    companion object {
        fun fromExpression(expression: FirExpression, session: FirSession): VariableInformation =
            VariableInformation(expression, expression.resolvedType.fullyExpandedType(session).lowerBoundIfFlexible())
    }
}

data class WhenBranchList(
    val conditions: Map<VariableSource, List<FirExpression>>,
) {
    fun mentions(variable: VariableSource): Boolean =
        conditions.contains(variable)

    operator fun get(variable: VariableSource): List<FirExpression>? =
        conditions[variable]

    operator fun plus(other: WhenBranchList): WhenBranchList =
        (conditions.keys + other.conditions.keys).associateWith { v ->
            conditions[v].orEmpty() + other.conditions[v].orEmpty()
        }.let(::WhenBranchList)

    companion object {
        fun empty(): WhenBranchList = WhenBranchList(emptyMap())

        fun from(variable: VariableSource, expression: FirExpression): WhenBranchList =
            WhenBranchList(mapOf(variable to listOf(expression)))
    }
}

sealed interface WhenChoice {
    fun coveredBy(session: FirSession, expression: FirExpression): Boolean
    fun toWhenMissingCase(): WhenMissingCase

    data object Null : WhenChoice {
        override fun coveredBy(session: FirSession, expression: FirExpression): Boolean = when (expression) {
            is FirEqualityOperatorCall -> when (expression.operation) {
                // == null
                FirOperation.EQ -> expression.arguments[1].resolvedType.isNullableNothing
                else -> false
            }
            is FirTypeOperatorCall -> when (expression.operation) {
                // is A?
                FirOperation.IS -> expression.conversionTypeRef.coneType.canBeNull
                // !is A!
                FirOperation.NOT_IS -> !expression.conversionTypeRef.coneType.canBeNull
                else -> false
            }
            else -> false
        }

        override fun toWhenMissingCase(): WhenMissingCase = WhenMissingCase.NullIsMissing
    }

    open class NotNull(open val type: ConeKotlinType) : WhenChoice {
        override fun coveredBy(session: FirSession, expression: FirExpression): Boolean = when (expression) {
            is FirEqualityOperatorCall -> when (expression.operation) {
                // != null
                FirOperation.NOT_EQ -> expression.arguments[1].resolvedType.isNullableNothing
                else -> false
            }
            is FirTypeOperatorCall -> {
                val typeToCheck = expression.conversionTypeRef.coneType
                when (expression.operation) {
                    // is A!  and  A supertype of T
                    FirOperation.IS -> !type.canBeNull && AbstractTypeChecker.isSubtypeOf(session.typeContext, type, typeToCheck)
                    else -> false
                }
            }
            else -> false
        }

        override fun toWhenMissingCase(): WhenMissingCase = WhenMissingCase.NotNullIsMissing
    }

    data class SealedClass(override val type: ConeKotlinType, val symbol: FirBasedSymbol<*>) : NotNull(type) {
        override fun coveredBy(session: FirSession, expression: FirExpression): Boolean {
            // check the not null part
            if (super.coveredBy(session, expression)) return true

            val (isPositive, symbolToCheck) = when (expression) {
                is FirEqualityOperatorCall -> {
                    val symbolToCheck = when (val argument = expression.arguments[1]) {
                        is FirResolvedQualifier -> {
                            val firClass = (argument.symbol as? FirRegularClassSymbol)?.fir
                            when (firClass?.classKind) {
                                ClassKind.OBJECT -> firClass.symbol
                                else -> firClass?.companionObjectSymbol
                            }
                        }
                        else -> argument.toResolvedCallableSymbol()?.takeIf { it.fir is FirEnumEntry }
                    }

                    when (expression.operation) {
                        FirOperation.EQ, FirOperation.IDENTITY -> true to symbolToCheck
                        FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> false to symbolToCheck
                        else -> true to null
                    }
                }
                is FirTypeOperatorCall -> {
                    val symbolToCheck = expression.conversionTypeRef.coneType.fullyExpandedType(session).toSymbol(session)
                    when (expression.operation) {
                        FirOperation.IS -> true to symbolToCheck
                        FirOperation.NOT_IS -> false to symbolToCheck
                        else -> true to null
                    }
                }
                else -> true to null
            }

            val subclassesOfCurrentMatch =
                (symbolToCheck as? FirRegularClassSymbol)?.collectSealedSubclassesAndEnumEntries(session)
                    ?: (symbolToCheck as? FirEnumEntrySymbol)?.let { listOf(it) }
                    ?: return false

            if (isPositive) {
                return symbol in subclassesOfCurrentMatch
            } else {
                val subclassesOfOriginalType =
                    (type.toSymbol(session) as? FirRegularClassSymbol)?.collectSealedSubclassesAndEnumEntries(session) ?: return false
                return symbol in subclassesOfOriginalType && symbol !in subclassesOfCurrentMatch
            }
        }

        override fun toWhenMissingCase(): WhenMissingCase = when (symbol) {
            is FirClassSymbol<*> -> WhenMissingCase.IsTypeCheckIsMissing(symbol.classId, symbol.fir.classKind.isSingleton)
            is FirVariableSymbol<*> -> WhenMissingCase.EnumCheckIsMissing(symbol.callableId)
            else -> WhenMissingCase.Unknown
        }
    }

    data class BooleanValue(val value: Boolean) : WhenChoice {
        @Suppress("UNCHECKED_CAST")
        override fun coveredBy(session: FirSession, expression: FirExpression): Boolean {
            // note: we know that the expression mentions the expression
            when (expression) {
                is FirTypeOperatorCall -> return false
                is FirEqualityOperatorCall -> {
                    val argument = expression.arguments[1]
                    if (argument !is FirConstExpression<*> || argument.kind != ConstantValueKind.Boolean) return false
                    val info = (argument as FirConstExpression<Boolean>).value
                    return when (expression.operation) {
                        FirOperation.EQ, FirOperation.IDENTITY -> value == info
                        FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> value != info
                        else -> false
                    }
                }
                is FirFunctionCall -> {
                    val operator = expression.toResolvedCallableSymbol() ?: return false
                    if (!operator.isOperator || operator.name != OperatorNameConventions.NOT) return false
                    return !value // value == false
                }
                else -> return value // value == true
            }
        }

        override fun toWhenMissingCase(): WhenMissingCase = when (value) {
            true -> WhenMissingCase.BooleanIsMissing.TrueIsMissing
            false -> WhenMissingCase.BooleanIsMissing.FalseIsMissing
        }
    }
}

fun ConeKotlinType.choices(session: FirSession): List<WhenChoice> {
    val symbol = this.toSymbol(session) as? FirRegularClassSymbol
    val notNullType = this.withNullability(ConeNullability.NOT_NULL, session.typeContext)
    val base = when {
        isNothingOrNullableNothing -> emptyList()
        isBooleanOrNullableBoolean -> listOf(WhenChoice.BooleanValue(true), WhenChoice.BooleanValue(false))
        symbol?.fir?.modality == Modality.SEALED || symbol?.fir?.classKind == ClassKind.ENUM_CLASS ->
            (symbol.collectSealedSubclassesAndEnumEntries(session) - symbol).map { WhenChoice.SealedClass(notNullType, it) }
        else -> listOf(WhenChoice.NotNull(notNullType))
    }
    return base + listOfNotNull(WhenChoice.Null.takeIf { canBeNull })
}

fun FirClassLikeSymbol<*>.collectSealedSubclassesAndEnumEntries(session: FirSession): List<FirBasedSymbol<*>> =
    collectSealedSubclasses(session) + collectEnumEntries(session)

fun FirClassLikeSymbol<*>.collectSealedSubclasses(session: FirSession): List<FirClassLikeSymbol<*>> = when {
    this is FirRegularClassSymbol && fir.modality == Modality.SEALED ->
        fir.getSealedClassInheritors(session)
            .mapNotNull { session.symbolProvider.getClassLikeSymbolByClassId(it) }
            .flatMap { it.collectSealedSubclasses(session) }
    else -> listOf(this)
}

// collect enum entries, going into potential sealed classes too (which could be enum at the end)
fun FirClassLikeSymbol<*>.collectEnumEntries(session: FirSession): Collection<FirEnumEntrySymbol> = when {
    this is FirRegularClassSymbol && fir.classKind == ClassKind.ENUM_CLASS ->
        fir.collectEnumEntries().map { it.symbol }
    this is FirRegularClassSymbol && fir.modality == Modality.SEALED ->
        fir.getSealedClassInheritors(session)
            .mapNotNull { session.symbolProvider.getClassLikeSymbolByClassId(it) }
            .flatMap { it.collectEnumEntries(session) }
    else -> emptyList()
}
