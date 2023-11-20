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

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        throw IllegalArgumentException("Should not be there")
    }

    /**
     * The synthetic call for the whole [whenExpression] might be not completed yet
     */
    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: Any?): FirStatement {
        val session = bodyResolveComponents.session
        val dataFlowAnalyzer = bodyResolveComponents.dataFlowAnalyzer
        val worker = FirWhenExhaustivenessTransformer2Worker(session, dataFlowAnalyzer)
        whenExpression.replaceExhaustivenessStatus(worker.processExhaustivenessCheck(whenExpression))
        session.enumWhenTracker?.reportEnumUsageInWhen(bodyResolveComponents.file.sourceFile?.path, worker.getSubjectType(whenExpression))
        return whenExpression
    }
}

class FirWhenExhaustivenessTransformer2Worker(
    private val session: FirSession,
    private val dataFlowAnalyzer: FirDataFlowAnalyzer? = null
) {
    fun getSubjectType(whenExpression: FirWhenExpression): ConeKotlinType? {
        val subjectType = whenExpression.subjectVariable?.returnTypeRef?.coneType
            ?: whenExpression.subject?.resolvedType
            ?: return null

        return subjectType.fullyExpandedType(session).lowerBoundIfFlexible()
    }

    fun processExhaustivenessCheck(whenExpression: FirWhenExpression): ExhaustivenessStatus {
        if (whenExpression.branches.any { it.condition is FirElseIfTrueCondition } || whenExpression.isTrueAndFalse()) {
            return ExhaustivenessStatus.ProperlyExhaustive
        }

        // compute the information about variables and expressions
        val variableInformation = mutableMapOf<VariableSource, VariableInformation>()
        // compute information about the subject
        val subject = whenExpression.subject
        if (subject != null) {
            val subjectType = getSubjectType(whenExpression)
            when {
                // bail out early if we don't know the type of the subject, or it's expect
                subjectType == null -> return ExhaustivenessStatus.NotExhaustive.unknownMissingCase(subject.source)
                subjectType.toRegularClassSymbol(session)?.isExpect == true ->
                    return ExhaustivenessStatus.NotExhaustive.unknownMissingCase(subject.source)
                subjectType.isNothing -> return ExhaustivenessStatus.ExhaustiveAsNothing
                else -> {
                    // add the subject information
                    variableInformation[VariableSource.Subject] = VariableInformation(subject, subjectType)
                }
            }
        }
        val stableExpressions = computeStableExpressions(whenExpression, variableInformation)
        if (variableInformation.isEmpty() || stableExpressions.isEmpty()) {
            return ExhaustivenessStatus.NotExhaustive.unknownMissingCase(subject?.source)
        }

        val problems = stableExpressions.checkExhaustiveness(variableInformation.toList())
        if (problems.isEmpty()) {
            return ExhaustivenessStatus.ProperlyExhaustive
        } else {
            val missingCases = problems.map { problem ->
                problem.map { (variable, choice) ->
                    val reportedExpression = when (variable) {
                        VariableSource.Subject -> null
                        else -> variableInformation[variable]?.originalExpression?.source
                    }
                    WhenMissingCaseFor(reportedExpression, choice.toWhenMissingCase())
                }
            }
            return ExhaustivenessStatus.NotExhaustive(missingCases)
        }
    }

    // this function performs a few things at the same time:
    // - adds information about the types
    // - flattens nested 'or' and 'and'
    // - returns [notUsefulBranch] if a non-stable variable or an unknown expression is mentioned
    private fun computeStableExpressions(
        whenExpression: FirWhenExpression,
        variableInformation: MutableMap<VariableSource, VariableInformation>,
    ): List<WhenBranchList> =
        whenExpression.branches.flatMap {
            computeStableExpressions(whenExpression, it.condition, variableInformation)
        }

    private fun computeStableExpressions(
        completeWhenExpression: FirWhenExpression,
        expression: FirExpression,
        variableInformation: MutableMap<VariableSource, VariableInformation>,
    ): List<WhenBranchList> =
        computeStableExpressionsUnwrapped(completeWhenExpression, expression.unwrapSmartcastExpression(), variableInformation)

    private fun computeStableExpressionsUnwrapped(
        completeWhenExpression: FirWhenExpression,
        expression: FirExpression,
        variableInformation: MutableMap<VariableSource, VariableInformation>,
    ): List<WhenBranchList> = when (expression) {
        is FirElseIfTrueCondition -> listOf(WhenBranchList.empty())
        is FirBinaryLogicExpression -> {
            val left = computeStableExpressions(completeWhenExpression, expression.leftOperand, variableInformation)
            val right = computeStableExpressions(completeWhenExpression, expression.rightOperand, variableInformation)
            when (expression.kind) {
                LogicOperationKind.OR -> left + right
                LogicOperationKind.AND -> left.flatMap { l -> right.map { r -> l + r } }
            }
        }
        is FirEqualityOperatorCall, is FirTypeOperatorCall -> {
            expression as FirCall
            getInformation(completeWhenExpression, expression.arguments[0], expression, variableInformation).orNotUseful()
        }
        is FirFunctionCall -> {
            val operator = expression.toResolvedCallableSymbol()
            val operatorOk = operator != null && operator.isOperator && operator.name == OperatorNameConventions.NOT
            val receiver = expression.dispatchReceiver
            if (!operatorOk || receiver == null) null.orNotUseful()
            else getInformation(completeWhenExpression, receiver, expression, variableInformation).orNotUseful()
        }
        else ->
            // lone Boolean variables
            getInformation(completeWhenExpression, expression, expression, variableInformation).orNotUseful()
    }

    private fun getInformation(
        completeWhenExpression: FirWhenExpression,
        variable: FirExpression,
        whole: FirExpression,
        variableInformation: MutableMap<VariableSource, VariableInformation>,
    ): WhenBranchList? {
        val unwrapped = variable.unwrapSmartcastExpression()
        if (unwrapped is FirWhenSubjectExpression && unwrapped.whenRef.value === completeWhenExpression) {
            // case in which we have the subject variable
            return WhenBranchList.from(VariableSource.Subject, whole)
        } else {
            // otherwise we should have a stable reference
            val realVar =
                dataFlowAnalyzer?.getVariableFromSmartcastInfo(unwrapped)
                    ?.takeIf { it.stability == PropertyStability.STABLE_VALUE }
                    ?: return null
            val source = VariableSource.Real(realVar)
            if (source !in variableInformation) { // add information the first time
                variableInformation[VariableSource.Real(realVar)] = VariableInformation.fromExpression(unwrapped, session)
            }
            return WhenBranchList.from(source, whole)
        }
    }

    private fun WhenBranchList?.orNotUseful(): List<WhenBranchList> = when (this) {
        null -> emptyList()
        else -> listOf(this)
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

    // compute all the possibilities for a given type
    private fun ConeKotlinType.choices(session: FirSession): Set<WhenChoice> = when (this) {
        is ConeIntersectionType -> {
            val intersectionChoices = intersectedTypes.map { it.choices(session) }
            val notRefinableChoices = intersectionChoices.filter { it.none(WhenChoice::canBeRefined) }
            if (notRefinableChoices.isNotEmpty()) {
                // if we have a list of possible sealed classes / enum entries, just intersect those
                notRefinableChoices.reduce(Set<WhenChoice>::intersect)
            } else {
                // otherwise take the union of possibilities as upper bound
                intersectionChoices.flatten().toSet()
            }
        }
        else -> {
            val symbol = this.toSymbol(session) as? FirRegularClassSymbol
            val notNullType = this.withNullability(ConeNullability.NOT_NULL, session.typeContext)
            val base = when {
                isNothingOrNullableNothing -> emptySet()
                isBooleanOrNullableBoolean -> setOf(WhenChoice.BooleanValue(true), WhenChoice.BooleanValue(false))
                symbol?.isInSealedOrEnumHierarchy(session) == true ->
                    symbol.collectSealedSubclassesAndEnumEntries(session).map { WhenChoice.SealedClass(notNullType, it) }.toSet()
                else -> setOf(WhenChoice.NotNull(notNullType))
            }
            base + setOfNotNull(WhenChoice.Null.takeIf { canBeNull(session) })
        }
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
    val canBeRefined: Boolean

    data object Null : WhenChoice {
        override val canBeRefined: Boolean = false

        override fun coveredBy(session: FirSession, expression: FirExpression): Boolean = when (expression) {
            is FirEqualityOperatorCall -> when (expression.operation) {
                // == null
                FirOperation.EQ -> expression.arguments[1].resolvedType.isNullableNothing
                else -> false
            }
            is FirTypeOperatorCall -> when (expression.operation) {
                // is A?
                FirOperation.IS -> expression.conversionTypeRef.coneType.canBeNull(session)
                // !is A!
                FirOperation.NOT_IS -> !expression.conversionTypeRef.coneType.canBeNull(session)
                else -> false
            }
            else -> false
        }

        override fun toWhenMissingCase(): WhenMissingCase = WhenMissingCase.NullIsMissing
    }

    open class NotNull(open val type: ConeKotlinType) : WhenChoice {
        override val canBeRefined: Boolean = true

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
                    FirOperation.IS -> !type.canBeNull(session) && AbstractTypeChecker.isSubtypeOf(session.typeContext, type, typeToCheck)
                    else -> false
                }
            }
            else -> false
        }

        override fun toString(): String = "NotNull(type=$type)"

        override fun toWhenMissingCase(): WhenMissingCase = WhenMissingCase.NotNullIsMissing
    }

    data class SealedClass(override val type: ConeKotlinType, val symbol: FirBasedSymbol<*>) : NotNull(type) {
        override val canBeRefined: Boolean = false

        override fun coveredBy(session: FirSession, expression: FirExpression): Boolean {
            // check the not null part
            if (super.coveredBy(session, expression)) return true

            val (isPositive, symbolToCheck) = computeInformation(session, expression) ?: return false

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

        private fun computeInformation(session: FirSession, expression: FirExpression): Pair<Boolean, FirBasedSymbol<*>>? {
            when (expression) {
                is FirEqualityOperatorCall -> {
                    val symbolToCheck = when (val argument = expression.arguments[1]) {
                        is FirResolvedQualifier -> {
                            val firClass = (argument.symbol as? FirRegularClassSymbol)?.fir
                            when (firClass?.classKind) {
                                ClassKind.OBJECT -> firClass.symbol
                                else -> firClass?.companionObjectSymbol
                            }
                        }
                        else -> argument.toResolvedCallableSymbol(session)?.takeIf { it.fir is FirEnumEntry }
                    } ?: return null

                    return when (expression.operation) {
                        FirOperation.EQ, FirOperation.IDENTITY -> true to symbolToCheck
                        FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> false to symbolToCheck
                        else -> null
                    }
                }
                is FirTypeOperatorCall -> {
                    val typeToCheck =
                        expression.conversionTypeRef.coneType.fullyExpandedType(session)
                            .withNullability(ConeNullability.NOT_NULL, session.typeContext)
                    val symbolToCheck = typeToCheck.toSymbol(session) ?: return null
                    return when (expression.operation) {
                        FirOperation.IS -> true to symbolToCheck
                        FirOperation.NOT_IS -> {
                            if (!AbstractTypeChecker.isSubtypeOf(session.typeContext, typeToCheck, type)) return null
                            false to symbolToCheck
                        }
                        else -> null
                    }
                }
                else -> return null
            }
        }

        override fun toWhenMissingCase(): WhenMissingCase = when (symbol) {
            is FirClassSymbol<*> -> WhenMissingCase.IsTypeCheckIsMissing(symbol.classId, symbol.fir.classKind.isSingleton)
            is FirVariableSymbol<*> -> WhenMissingCase.EnumCheckIsMissing(symbol.callableId)
            else -> WhenMissingCase.Unknown
        }
    }

    data class BooleanValue(val value: Boolean) : WhenChoice {
        override val canBeRefined: Boolean = false

        @Suppress("UNCHECKED_CAST")
        override fun coveredBy(session: FirSession, expression: FirExpression): Boolean {
            // note: we know that the expression mentions the expression
            when (expression) {
                is FirTypeOperatorCall -> return false
                is FirEqualityOperatorCall -> {
                    val argument = expression.arguments[1]
                    if (argument !is FirLiteralExpression<*> || argument.kind != ConstantValueKind.Boolean) return false
                    val info = (argument as FirLiteralExpression<Boolean>).value
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

// special case of when { true -> ..., false -> ... }
@Suppress("UNCHECKED_CAST")
private fun FirWhenExpression.isTrueAndFalse(): Boolean {
    if (subject != null) return false
    val covered = mutableListOf<Boolean>()
    for (branch in branches) {
        val condition = branch.condition
        if (condition !is FirLiteralExpression<*>) continue
        if (condition.kind != ConstantValueKind.Boolean) continue
        covered.add((condition as FirLiteralExpression<Boolean>).value)
    }
    return true in covered && false in covered
}

fun FirClassLikeSymbol<*>.collectSealedSubclassesAndEnumEntries(session: FirSession): Set<FirBasedSymbol<*>> = when {
    this is FirRegularClassSymbol && fir.classKind == ClassKind.ENUM_CLASS ->
        fir.collectEnumEntries().map { it.symbol }.toSet()
    this is FirRegularClassSymbol && fir.modality == Modality.SEALED ->
        fir.getSealedClassInheritors(session)
            .mapNotNull { session.symbolProvider.getClassLikeSymbolByClassId(it) }
            .flatMap { it.collectSealedSubclassesAndEnumEntries(session) }
            .toSet()
    else -> setOf(this)
}

fun FirClassSymbol<*>.isInSealedOrEnumHierarchy(session: FirSession): Boolean =
    isSealedOrEnum || resolvedSuperTypes.mapNotNull { it.toSymbol(session) as? FirClassSymbol<*> }
        .any { it.isInSealedOrEnumHierarchy(session) }

val FirClassSymbol<*>.isSealedOrEnum: Boolean
    get() = fir.modality == Modality.SEALED || fir.classKind == ClassKind.ENUM_CLASS
