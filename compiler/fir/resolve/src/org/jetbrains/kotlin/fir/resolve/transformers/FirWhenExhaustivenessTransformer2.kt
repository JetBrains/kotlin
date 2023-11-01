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
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.dfa.PropertyStability
import org.jetbrains.kotlin.fir.resolve.dfa.RealVariable
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions

class FirWhenExhaustivenessTransformer2(private val bodyResolveComponents: BodyResolveComponents) : FirTransformer<Any?>() {

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
            FirWhenExhaustivenessTransformer.getSubjectType(bodyResolveComponents.session, whenExpression)
        )
        return whenExpression
    }

    private fun processExhaustivenessCheck(whenExpression: FirWhenExpression) {
        if (whenExpression.branches.any { it.condition is FirElseIfTrueCondition }) {
            whenExpression.replaceExhaustivenessStatus(ExhaustivenessStatus.ProperlyExhaustive)
            return
        }

        // compute the information about variables and expressions
        val realVariables = mutableMapOf<RealVariable, VariableInformation>()
        val stableExpressions = whenExpression.branches.flatMap { computeStableExpressions(it.condition, realVariables) }

        if (realVariables.all { (_, info) -> info.type.isNothing }) {
            whenExpression.replaceExhaustivenessStatus(ExhaustivenessStatus.ExhaustiveAsNothing)
            return
        }
        if (stableExpressions.isEmpty()) {
            whenExpression.replaceExhaustivenessStatus(ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH)
            return
        }

        val problems: List<List<Pair<RealVariable, WhenChoice>>> = stableExpressions.checkExhaustiveness(realVariables.toList())
        val missingCases = problems.map { problem ->
            problem.map { (variable, choice) ->
                val originalExpression = realVariables[variable]?.originalExpression
                val reportedExpression = originalExpression.takeIf { it !is FirWhenSubjectExpression }?.source
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
        expression: FirExpression,
        result: MutableMap<RealVariable, VariableInformation>
    ): List<WhenBranchList> {
        when (expression) {
            is FirElseIfTrueCondition -> return listOf(WhenBranchList.empty())
            is FirBinaryLogicExpression -> {
                val left = computeStableExpressions(expression.leftOperand, result)
                val right = computeStableExpressions(expression.rightOperand, result)
                return when (expression.kind) {
                    LogicOperationKind.OR -> left + right
                    LogicOperationKind.AND -> return left.flatMap { l -> right.map { r -> l + r } }
                }
            }
            is FirEqualityOperatorCall, is FirTypeOperatorCall -> {
                expression as FirCall
                return getInformation(expression.arguments[0], expression, result)?.let { listOf(it) } ?: notUsefulBranch
            }
            is FirFunctionCall -> {
                val operator = expression.toResolvedCallableSymbol() ?: return notUsefulBranch
                if (!operator.isOperator || operator.name != OperatorNameConventions.NOT) return notUsefulBranch
                val receiver = expression.dispatchReceiver ?: return notUsefulBranch
                return getInformation(receiver, expression, result)?.let { listOf(it) } ?: notUsefulBranch
            }
            else -> {
                // lone Boolean variables
                return getInformation(expression, expression, result)?.let { listOf(it) } ?: notUsefulBranch
            }
        }
    }

    private fun getInformation(
        variable: FirExpression,
        whole: FirExpression,
        result: MutableMap<RealVariable, VariableInformation>
    ): WhenBranchList? {
        val realVar = bodyResolveComponents.dataFlowAnalyzer.getVariableFromSmartcastInfo(variable) ?: return null
        if (realVar.stability != PropertyStability.STABLE_VALUE) return null
        result[realVar] = VariableInformation.fromExpression(variable, bodyResolveComponents.session)
        return WhenBranchList.from(realVar, whole)
    }

    private fun List<WhenBranchList>.checkExhaustiveness(
        variables: List<Pair<RealVariable, VariableInformation>>,
    ): List<MutableList<Pair<RealVariable, WhenChoice>>> {
        if (variables.isEmpty()) return mutableListOf()
        val (currentVariable, currentInfo) = variables.first()
        val restOfVariables = variables.drop(1)
        // if it's not mentioned, let's not even consider that one
        if (none { it.mentions(currentVariable) }) return checkExhaustiveness(restOfVariables)
        // now let's build the problems by checking each possible choice
        val exhaustivenessProblems: MutableList<MutableList<Pair<RealVariable, WhenChoice>>> = mutableListOf()
        for (currentChoice in currentInfo.type.choices(bodyResolveComponents.session)) {
            val elements = filter { branch ->
                branch[currentVariable]?.all { currentChoice.coveredBy(bodyResolveComponents.session, it) } ?: true
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
}

data class VariableInformation(
    val originalExpression: FirExpression,
    val type: ConeKotlinType
) {
    companion object {
        fun fromExpression(expression: FirExpression, session: FirSession): VariableInformation =
            VariableInformation(expression, expression.resolvedType.fullyExpandedType(session))
    }
}

data class WhenBranchList(
    val conditions: Map<RealVariable, List<FirExpression>>,
) {
    fun mentions(variable: RealVariable): Boolean =
        conditions.contains(variable)

    operator fun get(variable: RealVariable): List<FirExpression>? =
        conditions[variable]

    operator fun plus(other: WhenBranchList): WhenBranchList =
        (conditions.keys + other.conditions.keys).associateWith { v ->
            conditions[v].orEmpty() + other.conditions[v].orEmpty()
        }.let(::WhenBranchList)

    companion object {
        fun empty(): WhenBranchList = WhenBranchList(emptyMap())

        fun from(variable: RealVariable, expression: FirExpression): WhenBranchList =
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
                FirOperation.IS -> expression.conversionTypeRef.coneType.isNullable
                // !is A!
                FirOperation.NOT_IS -> !expression.conversionTypeRef.coneType.isNullable
                else -> false
            }
            else -> false
        }

        override fun toWhenMissingCase(): WhenMissingCase = WhenMissingCase.NullIsMissing
    }

    data object NotNull : WhenChoice {
        override fun coveredBy(session: FirSession, expression: FirExpression): Boolean = when (expression) {
            is FirEqualityOperatorCall -> when (expression.operation) {
                // != null
                FirOperation.NOT_EQ -> expression.arguments[1].resolvedType.isNullableNothing
                else -> false
            }
            // we don't get information from anything else, since we don't check the type
            else -> false
        }

        override fun toWhenMissingCase(): WhenMissingCase = WhenMissingCase.NotNullIsMissing
    }

    data class SealedClass(val symbol: FirBasedSymbol<*>) : WhenChoice {
        override fun coveredBy(session: FirSession, expression: FirExpression): Boolean = when (expression) {
            is FirEqualityOperatorCall -> {
                val symbolToCheck = when (val argument = expression.arguments[1]) {
                    is FirResolvedQualifier -> {
                        val firClass = (argument.symbol as? FirRegularClassSymbol)?.fir
                        if (firClass?.classKind == ClassKind.OBJECT) {
                            firClass.symbol
                        } else {
                            firClass?.companionObjectSymbol
                        }
                    }
                    else -> argument.toResolvedCallableSymbol()?.takeIf { it.fir is FirEnumEntry }
                }

                when (expression.operation) {
                    FirOperation.EQ, FirOperation.IDENTITY -> symbol == symbolToCheck
                    FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> symbol != symbolToCheck
                    else -> false
                }
            }
            is FirTypeOperatorCall -> {
                val symbolToCheck = expression.conversionTypeRef.coneType.fullyExpandedType(session).toSymbol(session)
                when (expression.operation) {
                    FirOperation.IS -> symbol == symbolToCheck
                    FirOperation.NOT_IS -> symbol != symbolToCheck
                    else -> false
                }
            }
            else -> false
        }

        override fun toWhenMissingCase(): WhenMissingCase = when (symbol) {
            is FirClassSymbol<*> -> WhenMissingCase.IsTypeCheckIsMissing(symbol.classId, symbol.fir.classKind.isSingleton)
            is FirVariableSymbol<*> -> WhenMissingCase.EnumCheckIsMissing(symbol.callableId)
            else -> WhenMissingCase.Unknown
        }
    }

    data class EnumEntry(val entry: FirEnumEntry) : WhenChoice {
        override fun coveredBy(session: FirSession, expression: FirExpression): Boolean = when (expression) {
            is FirEqualityOperatorCall -> {
                val argument = expression.arguments[1]
                val symbolToCheck = argument.toResolvedCallableReference()?.resolvedSymbol as? FirVariableSymbol<*>
                val entryToCheck = symbolToCheck?.fir as? FirEnumEntry
                when (expression.operation) {
                    FirOperation.EQ, FirOperation.IDENTITY -> entry == entryToCheck
                    FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> entry != entryToCheck
                    else -> false
                }
            }
            else -> false
        }

        override fun toWhenMissingCase(): WhenMissingCase = WhenMissingCase.EnumCheckIsMissing(entry.symbol.callableId)
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
            true -> WhenMissingCase.BooleanIsMissing.FalseIsMissing
            false -> WhenMissingCase.BooleanIsMissing.FalseIsMissing
        }
    }
}

fun ConeKotlinType.choices(session: FirSession): List<WhenChoice> {
    val symbol = this.toSymbol(session) as? FirRegularClassSymbol
    val base = when {
        isBoolean -> listOf(WhenChoice.BooleanValue(true), WhenChoice.BooleanValue(false))
        symbol?.fir?.modality == Modality.SEALED ->
            symbol.fir.getSealedClassInheritors(session)
                .mapNotNull { session.symbolProvider.getClassLikeSymbolByClassId(it) }
                .map { WhenChoice.SealedClass(it) }
        symbol?.fir?.classKind == ClassKind.ENUM_CLASS ->
            symbol.fir.collectEnumEntries().map { WhenChoice.EnumEntry(it) }
        else -> emptyList()
    }
    return base.ifEmpty { listOf(WhenChoice.NotNull) } + listOfNotNull(WhenChoice.Null.takeIf { isNullable })
}
