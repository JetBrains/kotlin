/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.FirEvaluatorResult.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.evaluatedInitializer
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.constants.evaluate.CompileTimeType
import org.jetbrains.kotlin.resolve.constants.evaluate.canEvalOp
import org.jetbrains.kotlin.resolve.constants.evaluate.evalBinaryOp
import org.jetbrains.kotlin.resolve.constants.evaluate.evalUnaryOp
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions

@RequiresOptIn(
    "Internal FirExpressionEvaluator API. Should be avoided because it can be changed or dropped anytime. " +
            "Consider using `evaluatePropertyInitializer` or `evaluateAnnotationArguments` instead."
)
annotation class PrivateConstantEvaluatorAPI

object FirExpressionEvaluator {
    /**
     * During constant evaluation, we can go from kotlin world to java world and back (see the example)
     *
     * Java constants are evaluated using PSI evaluator, which knows nothing about this particular class and no context is provided
     *   to its entrypoint. So we should somehow track cases of recursion without any context
     *
     * Since [EvaluationVisitor] should be side-effect free, we can't write anything directly in the tree, so it was decided
     *   to collect the list of properties which were already checked on the constant evaluator path in the thread-local list.
     *   So when the execution comes back to kotlin after java, this context will be preserved
     * ```
     * // FILE: Bar.java
     * public class Bar {
     *     public static final int BAR = TestKt.recursion2 + 1;
     * }
     *
     * // FILE: Test.kt
     * const val a = recursion1 + 1
     *
     * const val recursion1 = Bar.BAR + 1
     * const val recursion2 = recursion1 + 1
     * ```
     *
     * This property cannot be converted into a non-thread-local as we cannot control the entire stack of the call.
     * For instance, we may jump through a Java class, so there is no other way to restore the previous stack.
     * [evaluatedInitializer][org.jetbrains.kotlin.fir.declarations.utils.evaluatedInitializer]
     * cannot be used for this purpose as the
     * [CONSTANT_EVALUATION][org.jetbrains.kotlin.fir.declarations.FirResolvePhase.CONSTANT_EVALUATION]
     * is non-jumping phase, so it cannot modify unrelated declarations.
     *
     * Code example:
     * ```
     * // FILE: KotlinClass.kt
     * class KotlinClass {
     *   companion object {
     *     const val foo: Int = JavaClass.javaField + 1
     *     const val bar: Int = foo + 1
     *   }
     * }
     *
     * // FILE: JavaClass.java
     * public class JavaClass {
     *     public static int javaField = KotlinClass.bar + 1;
     * }
     * ```
     */
    private val visitedCallables: ThreadLocal<HashSet<FirCallableSymbol<*>>> = ThreadLocal.withInitial(::hashSetOf)

    fun evaluatePropertyInitializer(property: FirProperty, session: FirSession, firFile: FirFile? = null): FirEvaluatorResult? {
        property.evaluatedInitializer?.let { return it }
        if (!property.isConst) {
            return null
        }

        return evaluateVariableValue(
            property,
            session,
            firFile,
            isAllowedType = { canBeUsedForConstVal() },
            value = { initializer }
        )
    }

    fun evaluateParameterDefaultValue(parameter: FirValueParameter, session: FirSession, firFile: FirFile? = null): FirEvaluatorResult? {
        return evaluateVariableValue(
            parameter,
            session,
            firFile,
            { true },
            { defaultValue }
        )
    }

    private inline fun <T : FirVariable> evaluateVariableValue(
        variable: T,
        session: FirSession,
        firFile: FirFile?,
        isAllowedType: ConeKotlinType.() -> Boolean,
        value: T.() -> FirExpression?,
    ): FirEvaluatorResult? {
        val type = variable.returnTypeRef.coneTypeOrNull?.fullyExpandedType(session)
        if (type == null || type is ConeErrorType || !type.isAllowedType()) {
            return null
        }

        val initializer = variable.value()
        if (initializer == null || !initializer.canBeEvaluated()) {
            return null
        }

        return initializer.evaluateAndAdjustType(session, firFile, variable)
    }

    fun evaluateAnnotationArguments(annotation: FirAnnotation, session: FirSession, firFile: FirFile? = null): Map<Name, FirEvaluatorResult> {
        val argumentMapping = annotation.argumentMapping.mapping
        val parameters = (annotation as? FirAnnotationCall)?.resolvedArgumentMapping?.values?.associate { it.name to it } ?: emptyMap()

        return argumentMapping.mapValues { [name, expression] ->
            expression.evaluateAndAdjustType(session, firFile, parameters[name])
        }
    }

    @PrivateConstantEvaluatorAPI
    fun evaluateExpression(expression: FirExpression, session: FirSession): FirEvaluatorResult? {
        if (!expression.canBeEvaluated()) return null
        return expression.evaluate(session, calledOnCheckerStage = true)
    }

    private fun FirExpression?.canBeEvaluated(): Boolean {
        return this != null && this !is FirLazyExpression && hasResolvedType
    }

    private fun FirExpression.evaluate(session: FirSession, firFile: FirFile? = null, calledOnCheckerStage: Boolean): FirEvaluatorResult {
        val visitor = EvaluationVisitor(session, firFile, calledOnCheckerStage)
        return this.accept(visitor, null)
    }

    private fun FirExpression.evaluateAndAdjustType(
        session: FirSession, firFile: FirFile? = null, variable: FirVariable?,
    ): FirEvaluatorResult {
        val evaluated = this.evaluate(session, firFile, calledOnCheckerStage = false)
        val expression = evaluated.resultOrNull<FirLiteralExpression>() ?: return evaluated

        // Convert literal expression to the variable's type
        val expectedType = variable?.returnTypeRef?.coneType ?: return evaluated
        return expression.value?.adjustTypeAndConvertToLiteral(expression, expectedType)?.wrap() ?: evaluated
    }

    private inline fun <T> FirCallableSymbol<*>.visit(block: () -> T): T {
        val visited = visitedCallables.get()
        visited.add(this)
        return try {
            block()
        } finally {
            visited.remove(this)
            if (visited.isEmpty()) {
                // to avoid keeping large empty collections in memory
                visitedCallables.remove()
            }
        }
    }

    private fun FirCallableSymbol<*>.wasVisited(): Boolean = this in visitedCallables.get()

    private class EvaluationVisitor(
        val session: FirSession,
        private val firFile: FirFile? = null,
        private val calledOnCheckerStage: Boolean = false,
    ) : FirVisitor<FirEvaluatorResult, Nothing?>() {
        private inline fun <reified T : FirElement> evaluateOr(element: FirElement?, action: (NotEvaluated) -> Nothing): T {
            val result = element?.accept(this, null) ?: NotConst
            when {
                result is NotEvaluated -> action(result)
                (result as Evaluated).result !is T -> action(NotConst)
                else -> return result.result as T
            }
        }

        override fun visitElement(element: FirElement, data: Nothing?): FirEvaluatorResult {
            return NotConst
        }

        override fun visitErrorExpression(errorExpression: FirErrorExpression, data: Nothing?): FirEvaluatorResult {
            // Error expression already signalizes about some problem, and later we will report some diagnostic.
            // Depending on the context, we can count this as valid or as error expression.
            // So we delegate the final decision to the caller.
            return ResolutionError
        }

        override fun visitLiteralExpression(literalExpression: FirLiteralExpression, data: Nothing?): FirEvaluatorResult {
            return literalExpression.wrap()
        }

        override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: Nothing?): FirEvaluatorResult {
            val classSymbol = thisReceiverExpression.calleeReference.boundSymbol as? FirClassSymbol
            if (classSymbol?.classKind == ClassKind.OBJECT) return thisReceiverExpression.wrap()
            return NotConst
        }

        override fun visitQualifiedAccessExpression(
            qualifiedAccessExpression: FirQualifiedAccessExpression, data: Nothing?,
        ): FirEvaluatorResult {
            val expressionType = qualifiedAccessExpression.getExpandedType(session)
            if (expressionType.isReflectFunctionType(session) || expressionType.isKProperty(session) || expressionType.isKMutableProperty(session)) {
                // Ignore the result of evaluation, it will not be used. We just want to check that the receiver is a constant expression.
                qualifiedAccessExpression.dispatchReceiver?.let { receiver -> evaluateOr<FirElement>(receiver) { return it } }
            }

            return qualifiedAccessExpression.calleeReference.resolved.wrap()
        }

        override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: Nothing?): FirEvaluatorResult {
            return visitQualifiedAccessExpression(callableReferenceAccess, data)
        }

        override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: Nothing?): FirEvaluatorResult {
            return resolvedNamedReference.wrap()
        }

        override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: Nothing?): FirEvaluatorResult {
            return resolvedQualifier.wrap()
        }

        override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier, data: Nothing?): FirEvaluatorResult {
            return errorResolvedQualifier.wrap()
        }

        override fun visitGetClassCall(getClassCall: FirGetClassCall, data: Nothing?): FirEvaluatorResult {
            var coneType = getClassCall.argument.getExpandedType(session)

            if (coneType is ConeErrorType)
                return NotConst

            while (coneType.classId == StandardClassIds.Array)
                coneType = (coneType.lowerBoundIfFlexible().typeArguments.first() as? ConeKotlinTypeProjection)?.type ?: break

            val argument = getClassCall.argument
            return when {
                coneType is ConeTypeParameterType -> KClassLiteralOfTypeParameterError
                argument is FirResolvedQualifier || argument is FirClassReferenceExpression -> getClassCall.wrap()
                else -> NotKClassLiteral
            }
        }

        override fun visitArgumentList(argumentList: FirArgumentList, data: Nothing?): FirEvaluatorResult {
            return when (argumentList) {
                is FirResolvedArgumentList -> buildResolvedArgumentList(
                    argumentList.originalArgumentList,
                    argumentList.mapping.mapKeysTo(LinkedHashMap()) { evaluateOr(it.key) { return it } },
                )
                else -> buildArgumentList {
                    source = argumentList.source
                    arguments.addAll(argumentList.arguments.map { evaluateOr(it) { return it } })
                }
            }.wrap()
        }

        override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: Nothing?): FirEvaluatorResult {
            return buildNamedArgumentExpression {
                source = namedArgumentExpression.source
                annotations.addAll(namedArgumentExpression.annotations)
                expression = evaluateOr<FirExpression>(namedArgumentExpression.expression) { return it }
                isSpread = namedArgumentExpression.isSpread
                name = namedArgumentExpression.name
            }.wrap()
        }

        @OptIn(UnresolvedExpressionTypeAccess::class)
        override fun visitCollectionLiteral(collectionLiteral: FirCollectionLiteral, data: Nothing?): FirEvaluatorResult {
            return buildCollectionLiteral {
                source = collectionLiteral.source
                coneTypeOrNull = collectionLiteral.coneTypeOrNull
                annotations.addAll(collectionLiteral.annotations)
                argumentList = evaluateOr(collectionLiteral.argumentList) { return it }
            }.wrap()
        }

        @OptIn(UnresolvedExpressionTypeAccess::class)
        override fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression, data: Nothing?): FirEvaluatorResult {
            return buildVarargArgumentsExpression {
                source = varargArgumentsExpression.source
                coneTypeOrNull = varargArgumentsExpression.coneTypeOrNull
                annotations.addAll(varargArgumentsExpression.annotations)
                arguments.addAll(varargArgumentsExpression.arguments.map { evaluateOr(it) { return it } })
                coneElementTypeOrNull = varargArgumentsExpression.coneElementTypeOrNull
            }.wrap()
        }

        override fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: Nothing?): FirEvaluatorResult {
            return buildSpreadArgumentExpression {
                source = spreadArgumentExpression.source
                annotations.addAll(spreadArgumentExpression.annotations)
                expression = evaluateOr(spreadArgumentExpression.expression) { return it }
            }.wrap()
        }

        override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: Nothing?): FirEvaluatorResult {
            val propertySymbol = propertyAccessExpression.calleeReference.toResolvedCallableSymbol()
            // Null symbol means some error occurred.
            // We use the same logic as in `visitErrorExpression`.
            // Better to report "UNRESOLVED_REFERENCE" later than some "NOT_CONST" diagnostic right now.
                ?: return ResolutionError

            if (propertySymbol.wasVisited()) {
                return RecursionInInitializer
            }

            fun evaluateWithSourceCopy(initializer: FirExpression?): FirEvaluatorResult = propertySymbol.visit {
                // We need a copy here to copy a source of the original expression
                if (initializer is FirLiteralExpression) {
                    initializer.copy(propertyAccessExpression).wrap()
                } else {
                    val evaluatedResult = evaluateOr<FirLiteralExpression>(initializer) { return it }
                    evaluatedResult.copy(propertyAccessExpression).wrap()
                }
            }

            return when (propertySymbol) {
                is FirPropertySymbol -> {
                    val validation = validate(propertySymbol, propertyAccessExpression)
                    if (validation != null) return validation

                    val isConstWithoutInitializer = propertySymbol.isCompileTimeBuiltinProperty(session)

                    when {
                        isConstWithoutInitializer -> when {
                            propertySymbol.callableId?.isStringLength == true || propertySymbol.callableId?.isCharCode == true -> {
                                val unaryArg = evaluateOr<FirExpression>(propertyAccessExpression.explicitReceiver) { return it }
                                evaluateUnary(unaryArg, propertySymbol.callableId!!)
                                    .adjustTypeAndConvertToLiteral(propertyAccessExpression)
                            }

                            // The `name` property will be evaluated only for `Enum` and `KCallable` objects.
                            // All other objects receive the default treatment.
                            propertySymbol.callableId?.callableName == StandardNames.NAME -> {
                                val result = evaluateOr<FirElement>(propertyAccessExpression.explicitReceiver) { return it }
                                when (result) {
                                    is FirPropertyAccessExpression -> {
                                        val name = result.calleeReference.name.asString()
                                        name.adjustTypeAndConvertToLiteral(propertyAccessExpression)
                                    }
                                    is FirResolvedCallableReference -> {
                                        val name = when (result.resolvedSymbol) {
                                            is FirConstructorSymbol -> SpecialNames.INIT.asString()
                                            else -> result.name.asString()
                                        }
                                        name.adjustTypeAndConvertToLiteral(propertyAccessExpression)
                                    }
                                    else -> NotConst
                                }
                            }
                            else -> NotConst
                        }
                        propertySymbol.isConst -> {
                            // Return only Evaluated value. For errors, we want to do it again to make it more precise.
                            propertySymbol.fir.evaluatedInitializer
                                ?.takeIf { it is Evaluated || !calledOnCheckerStage }
                                ?.copy(propertyAccessExpression)
                                ?.let { return it }
                            evaluateWithSourceCopy(propertySymbol.resolvedInitializer)
                        }
                        else -> NotConst
                    }
                }
                is FirFieldSymbol -> {
                    if (!propertySymbol.isStatic || propertySymbol.modality != Modality.FINAL || !propertySymbol.hasConstantInitializer) {
                        return NotConst
                    }
                    evaluateWithSourceCopy(propertySymbol.resolvedInitializer).apply {
                        session.inlineConstTracker.report(propertySymbol.fir, firFile, this)
                    }
                }
                is FirEnumEntrySymbol -> propertyAccessExpression.wrap()
                else -> NotConst
            }
        }

        private fun validate(propertySymbol: FirPropertySymbol, propertyAccessExpression: FirPropertyAccessExpression): NotEvaluated? {
            // Check for the resolved type. In case of cyclic resolution error, we will get an exception from `getReferencedClassSymbol`.
            if (propertySymbol.fir.returnTypeRef !is FirResolvedTypeRef) return NotConst

            val classKindOfParent = (propertySymbol.getReferencedClassSymbol(session) as? FirRegularClassSymbol)?.classKind
            if (classKindOfParent == ClassKind.ENUM_CLASS) return EnumNotConst

            val isConstWithoutInitializer = propertySymbol.isCompileTimeBuiltinProperty(session)

            return when {
                propertySymbol is FirLocalPropertySymbol -> NotConst
                propertyAccessExpression.getExpandedType(session).classId == StandardClassIds.KClass -> NotKClassLiteral
                isConstWithoutInitializer -> when {
                    propertySymbol.callableId?.isStringLength == true || propertySymbol.callableId?.isCharCode == true -> null
                    propertySymbol.callableId?.callableName == StandardNames.NAME -> null
                    else -> NotConst
                }
                propertySymbol.isConst -> {
                    // even if called on CONSTANT_EVALUATION, it's safe to call resolvedInitializer, as intializers of const vals
                    // are resolved at previous IMPLICIT_TYPES_BODY_RESOLVE phase
                    if (propertySymbol.resolvedInitializer == null) return ResolutionError

                    val receivers = listOf(propertyAccessExpression.dispatchReceiver, propertyAccessExpression.extensionReceiver)
                    if (receivers.count { it != null } == 2) return NotConst

                    // We are not interested in the result, but we must check the receivers anyway
                    receivers.filterNotNull().forEach { receiver -> evaluateOr<FirElement>(receiver) { return it } }

                    null
                }
                !calledOnCheckerStage -> NotConst
                // if it called at checkers stage it's safe to call resolvedInitializer
                // even if it will trigger BODY_RESOLVE phase, we don't violate phase contracts
                propertySymbol.resolvedInitializer is FirLiteralExpression -> when {
                    propertySymbol.isVal -> NotConstValInConstExpression
                    else -> NotConst
                }
                propertySymbol.resolvedInitializer is FirGetClassCall -> NotKClassLiteral
                else -> NotConst
            }
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall, data: Nothing?): FirEvaluatorResult {
            val calleeReference = functionCall.calleeReference
            if (calleeReference !is FirResolvedNamedReference) return NotConst
            if (functionCall.getExpandedType(session).classId == StandardClassIds.KClass) return NotKClassLiteral

            return when (val symbol = calleeReference.resolvedSymbol) {
                is FirNamedFunctionSymbol -> visitNamedFunction(functionCall, symbol)
                is FirConstructorSymbol -> visitConstructorCall(functionCall)
                else -> NotConst
            }
        }

        private fun visitNamedFunction(functionCall: FirFunctionCall, symbol: FirNamedFunctionSymbol): FirEvaluatorResult {
            if (!functionCall.isCompileTimeBuiltinCall(session)) return NotConst

            val receivers = listOfNotNull(functionCall.dispatchReceiver, functionCall.extensionReceiver)
            val evaluatedArgs = receivers.plus(functionCall.arguments).map {
                if (!it.hasAllowedCompileTimeType(session)) return NotConst
                evaluateOr<FirLiteralExpression>(it) { return it }
            }

            return when (evaluatedArgs.size) {
                1 -> evaluateUnary(evaluatedArgs.first(), symbol.callableId)
                    ?.adjustTypeAndConvertToLiteral(functionCall)
                    ?: NotConst
                2 -> evaluateBinary(evaluatedArgs.first(), symbol.callableId, evaluatedArgs.get(1))
                    ?.adjustTypeAndConvertToLiteral(functionCall)
                    ?: NotConst
                else -> NotConst
            }
        }

        @OptIn(UnresolvedExpressionTypeAccess::class)
        private fun visitConstructorCall(constructorCall: FirFunctionCall): FirEvaluatorResult {
            val type = constructorCall.resolvedType.fullyExpandedType(session).lowerBoundIfFlexible()
            when {
                type.toRegularClassSymbol(session)?.classKind == ClassKind.ANNOTATION_CLASS -> {
                    val evaluatedArgs = evaluateOr<FirResolvedArgumentList>(constructorCall.argumentList) { return it }
                    return buildFunctionCall {
                        coneTypeOrNull = constructorCall.coneTypeOrNull
                        annotations.addAll(constructorCall.annotations)
                        typeArguments.addAll(constructorCall.typeArguments)
                        source = constructorCall.source
                        nonFatalDiagnostics.addAll(constructorCall.nonFatalDiagnostics)
                        argumentList = evaluatedArgs
                        calleeReference = constructorCall.calleeReference
                        origin = constructorCall.origin
                    }.wrap()
                }
                type.isUnsignedType -> {
                    val argument = (evaluateOr<FirLiteralExpression>(constructorCall.argument) { return it }).value
                    return argument.adjustTypeAndConvertToLiteral(constructorCall)
                }
                else -> return NotConst
            }
        }

        override fun visitIntegerLiteralOperatorCall(
            integerLiteralOperatorCall: FirIntegerLiteralOperatorCall,
            data: Nothing?,
        ): FirEvaluatorResult {
            return visitFunctionCall(integerLiteralOperatorCall, data)
        }

        override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: Nothing?): FirEvaluatorResult {
            val evaluated = evaluateOr<FirLiteralExpression>(comparisonExpression.compareToCall) { return it }
            val intResult = evaluated.value as? Int ?: return NotConst
            val compareToResult = when (comparisonExpression.operation) {
                FirOperation.LT -> intResult < 0
                FirOperation.LT_EQ -> intResult <= 0
                FirOperation.GT -> intResult > 0
                FirOperation.GT_EQ -> intResult >= 0
                else -> NotConst
            }
            return compareToResult.adjustTypeAndConvertToLiteral(comparisonExpression)
        }

        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: Nothing?): FirEvaluatorResult {
            if (equalityOperatorCall.operation == FirOperation.IDENTITY || equalityOperatorCall.operation == FirOperation.NOT_IDENTITY) {
                return NotConst
            }

            val evaluatedArgs = equalityOperatorCall.arguments.map {
                if (!it.hasAllowedCompileTimeType(session) || (!session.intrinsicConstEvaluationEnabled && it.getExpandedType(session).isUnsignedType)) {
                    return NotConst
                }
                evaluateOr<FirLiteralExpression>(it) { return it }
            }
            if (evaluatedArgs.size != 2) return NotConst
            val opr1 = evaluatedArgs[0]
            val opr2 = evaluatedArgs[1]

            val opr1Value = opr1.kind.convertToGivenKind(opr1.value)
            val opr2Value = opr2.kind.convertToGivenKind(opr2.value)

            val result = when (equalityOperatorCall.operation) {
                FirOperation.EQ -> opr1Value == opr2Value
                FirOperation.NOT_EQ -> opr1Value != opr2Value
                else -> NotConst
            }

            return result.toConstExpression(ConstantValueKind.Boolean, equalityOperatorCall).wrap()
        }

        override fun visitBooleanOperatorExpression(booleanOperatorExpression: FirBooleanOperatorExpression, data: Nothing?): FirEvaluatorResult {
            if (!booleanOperatorExpression.leftOperand.resolvedType.isBoolean || !booleanOperatorExpression.rightOperand.resolvedType.isBoolean) {
                return NotConst
            }

            val left = evaluateOr<FirLiteralExpression>(booleanOperatorExpression.leftOperand) { return it }
            val right = evaluateOr<FirLiteralExpression>(booleanOperatorExpression.rightOperand) { return it }

            val leftBoolean = left.value as? Boolean ?: return NotConst
            val rightBoolean = right.value as? Boolean ?: return NotConst
            val result = when (booleanOperatorExpression.kind) {
                LogicOperationKind.AND -> leftBoolean && rightBoolean
                LogicOperationKind.OR -> leftBoolean || rightBoolean
            }

            return result.toConstExpression(ConstantValueKind.Boolean, booleanOperatorExpression).wrap()
        }

        override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: Nothing?): FirEvaluatorResult {
            val strings = stringConcatenationCall.arguments.map {
                // `null` is allowed
                if (!it.isNullLiteral && !it.hasAllowedCompileTimeType(session)) return NotConst
                evaluateOr<FirLiteralExpression>(it) { return it }
            }
            val result = strings.joinToString(separator = "") {
                it.kind.convertToGivenKind(it.value).toString()
            }
            return result.toConstExpression(ConstantValueKind.String, stringConcatenationCall).wrap()
        }

        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Nothing?): FirEvaluatorResult {
            if (typeOperatorCall.operation != FirOperation.AS) return NotConst
            val result = evaluateOr<FirLiteralExpression>(typeOperatorCall.argument) { return it }
            if (result.resolvedType.isSubtypeOf(typeOperatorCall.resolvedType, session)) {
                return result.wrap()
            }
            return NotConst
        }

        override fun visitEnumEntryDeserializedAccessExpression(
            enumEntryDeserializedAccessExpression: FirEnumEntryDeserializedAccessExpression,
            data: Nothing?,
        ): FirEvaluatorResult {
            return enumEntryDeserializedAccessExpression.wrap()
        }

        override fun visitClassReferenceExpression(
            classReferenceExpression: FirClassReferenceExpression,
            data: Nothing?,
        ): FirEvaluatorResult {
            return classReferenceExpression.wrap()
        }

        override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: Nothing?): FirEvaluatorResult {
            return visitAnnotation(annotationCall, data)
        }

        override fun visitAnnotation(annotation: FirAnnotation, data: Nothing?): FirEvaluatorResult {
            val mapping = annotation.argumentMapping.mapping
            if (mapping.isEmpty()) return annotation.wrap()
            val evaluatedMapping = mutableMapOf<Name, FirExpression>()
            for ([name, expression] in mapping) {
                evaluatedMapping[name] = evaluateOr<FirExpression>(expression) { return it }
            }
            return buildAnnotationCopy(annotation) {
                argumentMapping = buildAnnotationArgumentMapping {
                    this.mapping.putAll(evaluatedMapping)
                }
            }.wrap()
        }
    }
}

fun ConeKotlinType.canBeUsedForConstVal(): Boolean = with(lowerBoundIfFlexible()) { isPrimitive || isString || isUnsignedType }

private val compileTimeFunctions = setOf(
    *OperatorNameConventions.SIMPLE_BINARY_OPERATION_NAMES.toTypedArray(),
    *OperatorNameConventions.SIMPLE_UNARY_OPERATION_NAMES.toTypedArray(),
    *OperatorNameConventions.SIMPLE_BITWISE_OPERATION_NAMES.toTypedArray(),
    OperatorNameConventions.COMPARE_TO
)

private val compileTimeExtensionFunctions = listOf("floorDiv", "mod", "code").mapTo(hashSetOf()) { Name.identifier(it) }

private val FirSession.intrinsicConstEvaluationEnabled: Boolean
    get() = languageVersionSettings.supportsFeature(LanguageFeature.IntrinsicConstEvaluation)

private fun FirBasedSymbol<*>.isIntrinsicConst(session: FirSession): Boolean {
    return this.hasAnnotation(StandardClassIds.Annotations.IntrinsicConstEvaluation, session)
}

private fun ConeKotlinType.toCompileTimeType(): CompileTimeType? {
    if (this.classId == StandardClassIds.Any) return CompileTimeType.ANY
    return this.classId?.toConstantValueKind()?.toCompileTimeType()
}

private fun FirExpression.hasAllowedCompileTimeType(session: FirSession): Boolean {
    // See visitErrorExpression for details. Here we count the type as valid and take a decision later.
    if (this is FirErrorExpression) return true

    val expType = resolvedType.unwrapToSimpleTypeUsingLowerBound().fullyExpandedType(session)
    // TODO, KT-59823: add annotation for allowed constant types
    return expType.classId in StandardClassIds.constantAllowedTypes && !expType.isMarkedNullable
}

private fun FirExpression.getExpandedType(session: FirSession) = resolvedType.fullyExpandedType(session)

private fun FirFunctionCall.isCompileTimeBuiltinCall(session: FirSession): Boolean {
    val calleeReference = this.calleeReference
    if (calleeReference !is FirResolvedNamedReference) return false

    val name = calleeReference.name
    val symbol = calleeReference.resolvedSymbol as? FirCallableSymbol
    if (!symbol.fromStdlib()) return false

    val receiverClassId = this.dispatchReceiver?.getExpandedType(session)?.classId

    if (symbol is FirFunctionSymbol<*> && session.intrinsicConstEvaluationEnabled) {
        val receiverType = symbol.dispatchReceiverType?.fullyExpandedType(session)?.toCompileTimeType()
            ?: symbol.resolvedReceiverType?.fullyExpandedType(session)?.toCompileTimeType()
        val firstArgType = symbol.valueParameterSymbols.firstOrNull()?.resolvedReturnType?.fullyExpandedType(session)?.toCompileTimeType()

        val inBuiltinMap = canEvalOp(
            callableId = symbol.callableId,
            typeA = receiverType,
            typeB = firstArgType
        )
        return inBuiltinMap
    }

    if (!symbol.fromKotlinPackage()) return false
    if (receiverClassId in StandardClassIds.unsignedTypes) return false

    if (
        name in compileTimeFunctions ||
        name in compileTimeExtensionFunctions ||
        name == OperatorNameConventions.TO_STRING ||
        name in OperatorNameConventions.NUMBER_CONVERSIONS
    ) return true

    if (calleeReference.name == OperatorNameConventions.GET && receiverClassId == StandardClassIds.String) return true

    return false
}

private fun FirPropertySymbol.isCompileTimeBuiltinProperty(session: FirSession): Boolean {
    val receiverType = dispatchReceiverType ?: resolvedReceiverTypeRef?.coneTypeSafe<ConeKotlinType>() ?: return false
    val receiverClassId = receiverType.fullyExpandedType(session).classId ?: return false

    if (session.intrinsicConstEvaluationEnabled) {
        val callableId = this.unwrapFakeOverrides().callableId ?: return false
        if (callableId.isEnumName || callableId.isKCallableName) return true // Evaluated manually
        val receiverConstType = receiverType.toCompileTimeType() ?: return false
        val inBuiltinMap = canEvalOp(
            callableId = callableId,
            typeA = receiverConstType,
            typeB = null
        )
        return inBuiltinMap
    }

    return when (name.asString()) {
        "length" -> receiverClassId == StandardClassIds.String
        "code" -> receiverClassId == StandardClassIds.Char
        else -> false
    }
}

private fun FirCallableSymbol<*>?.fromStdlib(): Boolean {
    return this?.callableId?.packageName?.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME) == true
}

private fun FirCallableSymbol<*>?.fromKotlinPackage(): Boolean {
    return this?.callableId?.packageName?.asString() == StandardNames.BUILT_INS_PACKAGE_NAME.asString()
}

private fun FirCallableSymbol<*>?.getReferencedClassSymbol(session: FirSession): FirBasedSymbol<*>? =
    this?.resolvedReturnTypeRef?.coneType?.toSymbol(session)

fun ConstantValueKind.toCompileTimeType(): CompileTimeType {
    return when (this) {
        ConstantValueKind.Byte -> CompileTimeType.BYTE
        ConstantValueKind.Short -> CompileTimeType.SHORT
        ConstantValueKind.Int -> CompileTimeType.INT
        ConstantValueKind.Long -> CompileTimeType.LONG
        ConstantValueKind.UnsignedByte -> CompileTimeType.UBYTE
        ConstantValueKind.UnsignedShort -> CompileTimeType.USHORT
        ConstantValueKind.UnsignedInt -> CompileTimeType.UINT
        ConstantValueKind.UnsignedLong -> CompileTimeType.ULONG
        ConstantValueKind.Double -> CompileTimeType.DOUBLE
        ConstantValueKind.Float -> CompileTimeType.FLOAT
        ConstantValueKind.Char -> CompileTimeType.CHAR
        ConstantValueKind.Boolean -> CompileTimeType.BOOLEAN
        ConstantValueKind.String -> CompileTimeType.STRING

        else -> CompileTimeType.ANY
    }
}

// Unary operators
private fun evaluateUnary(arg: FirExpression, callableId: CallableId): Any? {
    if (arg !is FirLiteralExpression || arg.value == null) return null

    val opr = arg.kind.convertToGivenKind(arg.value) ?: return null
    return evalUnaryOp(
        callableId.callableName.asString(),
        arg.kind.toCompileTimeType(),
        opr
    )
}

// Binary operators
private fun evaluateBinary(
    arg1: FirExpression,
    callableId: CallableId,
    arg2: FirExpression,
): Any? {
    if (arg1 !is FirLiteralExpression || arg1.value == null) return null
    if (arg2 !is FirLiteralExpression || arg2.value == null) return null
    // NB: some utils accept very general types, and due to the way operation map works, we should up-cast rhs type.
    val rightType = when {
        callableId.isEquals -> CompileTimeType.ANY
        callableId.isStringPlus -> CompileTimeType.ANY
        else -> arg2.kind.toCompileTimeType()
    }

    val leftType = arg1.kind.toCompileTimeType()

    val opr1 = arg1.kind.convertToGivenKind(arg1.value) ?: return null
    val opr2 = arg2.kind.convertToGivenKind(arg2.value) ?: return null

    val functionName = callableId.callableName.asString()

    // Check for division by zero
    if (functionName == "div" || functionName == "rem") {
        if (!leftType.isFloatingPoint() && !rightType.isFloatingPoint() && (opr2 as? Number)?.toInt() == 0) {
            // If expression is division by zero, then return the original expression as a result. We will handle on later steps.
            return DivisionByZero
        }
    }

    // Check for trimMargin invalid argument
    if (functionName == "trimMargin" && (opr2 as? String)?.isBlank() == true) {
        return TrimMarginBlankPrefix
    }

    return evalBinaryOp(
        functionName,
        arg1.kind.toCompileTimeType(),
        opr1,
        rightType,
        opr2
    )
}

private fun Any?.adjustTypeAndConvertToLiteral(original: FirExpression): FirEvaluatorResult {
    if (this == null) return NotConst
    if (this is FirEvaluatorResult) return this
    return adjustTypeAndConvertToLiteral(original, original.resolvedType)?.wrap() ?: NotConst
}

private fun Any.adjustTypeAndConvertToLiteral(original: FirExpression, expectedType: ConeKotlinType): FirLiteralExpression? {
    val expectedKind = expectedType.toConstantValueKind() ?: return null
    val typeAdjustedValue = expectedKind.convertToGivenKind(this) ?: return null
    return typeAdjustedValue.toConstExpression(expectedKind, original)
}

private val CallableId.isStringLength: Boolean
    get() = classId == StandardClassIds.String && callableName.identifierOrNullIfSpecial == "length"

private val CallableId.isEquals: Boolean
    get() = callableName == OperatorNameConventions.EQUALS

private val CallableId.isStringPlus: Boolean
    get() = classId == StandardClassIds.String && callableName == OperatorNameConventions.PLUS

private val CallableId.isCharCode: Boolean
    get() = packageName == StandardClassIds.BASE_KOTLIN_PACKAGE && classId == null && callableName.identifierOrNullIfSpecial == "code"

private val CallableId.isKCallableName: Boolean
    get() = packageName == StandardClassIds.BASE_REFLECT_PACKAGE && classId == StandardClassIds.KCallable && callableName.identifierOrNullIfSpecial == "name"

private val CallableId.isEnumName: Boolean
    get() = packageName == StandardClassIds.BASE_KOTLIN_PACKAGE && classId == StandardClassIds.Enum && callableName.identifierOrNullIfSpecial == "name"

////// KINDS

private fun ConeKotlinType.toConstantValueKind(): ConstantValueKind? =
    when (this) {
        is ConeErrorType -> null
        is ConeLookupTagBasedType -> (lookupTag as? ConeClassLikeLookupTag)?.classId?.toConstantValueKind()
        is ConeFlexibleType -> upperBound.toConstantValueKind()
        is ConeCapturedType -> constructor.lowerType?.toConstantValueKind() ?: constructor.supertypes!!.first().toConstantValueKind()
        is ConeDefinitelyNotNullType -> original.toConstantValueKind()
        is ConeIntersectionType -> intersectedTypes.first().toConstantValueKind()
        is ConeStubType, is ConeIntegerLiteralType, is ConeTypeVariableType -> null
    }

fun ClassId.toConstantValueKind(): ConstantValueKind? =
    when (this) {
        StandardClassIds.Byte -> ConstantValueKind.Byte
        StandardClassIds.Double -> ConstantValueKind.Double
        StandardClassIds.Float -> ConstantValueKind.Float
        StandardClassIds.Int -> ConstantValueKind.Int
        StandardClassIds.Long -> ConstantValueKind.Long
        StandardClassIds.Short -> ConstantValueKind.Short

        StandardClassIds.Char -> ConstantValueKind.Char
        StandardClassIds.String -> ConstantValueKind.String
        StandardClassIds.Boolean -> ConstantValueKind.Boolean

        StandardClassIds.UByte -> ConstantValueKind.UnsignedByte
        StandardClassIds.UShort -> ConstantValueKind.UnsignedShort
        StandardClassIds.UInt -> ConstantValueKind.UnsignedInt
        StandardClassIds.ULong -> ConstantValueKind.UnsignedLong

        else -> null
    }

private fun ConstantValueKind.convertToGivenKind(value: Any?): Any? {
    if (value == null) {
        return null
    }
    return when (this) {
        ConstantValueKind.Boolean -> value as? Boolean
        ConstantValueKind.Char -> value as? Char
        ConstantValueKind.String -> value as? String
        ConstantValueKind.Byte -> (value as? Number)?.toByte()
        ConstantValueKind.Double -> (value as? Number)?.toDouble()
        ConstantValueKind.Float -> (value as? Number)?.toFloat()
        ConstantValueKind.Int -> (value as? Number)?.toInt()
        ConstantValueKind.Long -> (value as? Number)?.toLong()
        ConstantValueKind.Short -> (value as? Number)?.toShort()
        ConstantValueKind.UnsignedByte -> {
            if (value is UByte) value
            else (value as? Number)?.toLong()?.toUByte()
        }
        ConstantValueKind.UnsignedShort -> {
            if (value is UShort) value
            else (value as? Number)?.toLong()?.toUShort()
        }
        ConstantValueKind.UnsignedInt -> {
            if (value is UInt) value
            else (value as? Number)?.toLong()?.toUInt()
        }
        ConstantValueKind.UnsignedLong -> {
            if (value is ULong) value
            else (value as? Number)?.toLong()?.toULong()
        }
        ConstantValueKind.UnsignedIntegerLiteral -> {
            when (value) {
                is UInt -> value.toULong()
                is ULong -> value
                else -> (value as? Number)?.toLong()?.toULong()
            }
        }
        else -> null
    }
}

private fun CompileTimeType.isFloatingPoint() = this == CompileTimeType.FLOAT || this == CompileTimeType.DOUBLE

private fun Any?.toConstExpression(
    kind: ConstantValueKind,
    originalExpression: FirExpression,
): FirLiteralExpression {
    // Later stages of the compiler expect signed values
    val value = when (this) {
        is UByte -> this.toByte()
        is UShort -> this.toShort()
        is UInt -> this.toInt()
        is ULong -> this.toLong()
        else -> this
    }

    return buildLiteralExpression(
        originalExpression.source,
        kind,
        value,
        originalExpression.annotations.takeIf { it.isNotEmpty() }?.toMutableList(),
        setType = false,
    ).apply { replaceConeTypeOrNull(originalExpression.resolvedType) }
}

private fun FirLiteralExpression.copy(originalExpression: FirExpression): FirLiteralExpression {
    return this.value.toConstExpression(this.kind, originalExpression)
}

private fun FirEvaluatorResult.copy(originalExpression: FirExpression): FirEvaluatorResult {
    if (this !is Evaluated) {
        return this
    }

    val unwrappedLiteralResult = result as? FirLiteralExpression ?: return this
    return unwrappedLiteralResult.copy(originalExpression).wrap()
}

private fun FirElement?.wrap(): FirEvaluatorResult {
    return if (this != null) Evaluated(this) else NotConst
}
