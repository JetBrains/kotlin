/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

fun ConeKotlinType.canBeUsedForConstVal(): Boolean = with(lowerBoundIfFlexible()) { isPrimitive || isString || isUnsignedType }

/**
 * See the documentation to [computeConstantExpressionKind] function below
 */
fun canBeEvaluatedAtCompileTime(
    expression: FirExpression?,
    session: FirSession,
    allowErrors: Boolean,
    calledOnCheckerStage: Boolean,
): Boolean {
    val result = computeConstantExpressionKind(expression, session, calledOnCheckerStage)
    return result == ConstantArgumentKind.VALID_CONST || allowErrors && result == ConstantArgumentKind.RESOLUTION_ERROR
}

/**
 * This function computes if given [expression] can be counted as a constant expression or not
 * It returns a [ConstantArgumentKind], which can be used to understand why exactly the expression is not constant
 *
 * Precise computation of this [ConstantArgumentKind] may require resolution of initializer of non-const properties, which is allowed
 *   to do only on BODY_RESOLVE phase and checkers phase. Without it, the result may be less precise but still correct (it may return
 *   the general [ConstantArgumentKind.NOT_CONST] instead more specific [ConstantArgumentKind.NOT_KCLASS_LITERAL] for example)
 *
 * So, to allow using this function not only from checkers there is a @param [calledOnCheckerStage], which should be set to [true] ONLY
 *   if this method is called from checkers
 */
fun computeConstantExpressionKind(
    expression: FirExpression?,
    session: FirSession,
    calledOnCheckerStage: Boolean
): ConstantArgumentKind {
    if (expression == null) return ConstantArgumentKind.RESOLUTION_ERROR
    return expression.accept(FirConstCheckVisitor(session, calledOnCheckerStage), null)
}

enum class ConstantArgumentKind {
    VALID_CONST,
    RESOLUTION_ERROR,
    NOT_CONST,
    ENUM_NOT_CONST,
    NOT_KCLASS_LITERAL,
    NOT_CONST_VAL_IN_CONST_EXPRESSION,
    KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR;

    inline fun ifNotValidConst(action: (ConstantArgumentKind) -> Unit) {
        if (this != VALID_CONST) {
            action(this)
        }
    }
}

private class FirConstCheckVisitor(
    private val session: FirSession,
    val calledOnCheckerStage: Boolean,
) : FirVisitor<ConstantArgumentKind, Nothing?>() {
    companion object {
        /**
         * During constant evaluation, we can go from kotlin world to java world and back (see the example)
         *
         * Java constants are evaluated using PSI evaluator, which knows nothing about this particular class and no context is provided
         *   to its entrypoint. So we should somehow track cases of recursion without any context
         *
         * Since [FirConstCheckVisitor] should be side-effect free, we can't write anything directly in the tree, so it was decided
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
         */
        private val propertyStack: ThreadLocal<MutableList<FirCallableSymbol<*>>> = ThreadLocal.withInitial { mutableListOf() }

        private val compileTimeFunctions = setOf(
            *OperatorNameConventions.SIMPLE_BINARY_OPERATION_NAMES.toTypedArray(),
            *OperatorNameConventions.SIMPLE_UNARY_OPERATION_NAMES.toTypedArray(),
            *OperatorNameConventions.SIMPLE_BITWISE_OPERATION_NAMES.toTypedArray(),
            OperatorNameConventions.COMPARE_TO
        )

        private val compileTimeExtensionFunctions = listOf("floorDiv", "mod", "code").mapTo(hashSetOf()) { Name.identifier(it) }
    }

    private val intrinsicConstEvaluation = session.languageVersionSettings.supportsFeature(LanguageFeature.IntrinsicConstEvaluation)

    private fun <T> FirCallableSymbol<*>.visit(block: () -> T?): T? {
        propertyStack.get() += this
        try {
            return block()
        } finally {
            propertyStack.get().removeLast()
        }
    }

    override fun visitElement(element: FirElement, data: Nothing?): ConstantArgumentKind {
        return ConstantArgumentKind.NOT_CONST
    }

    override fun visitErrorExpression(errorExpression: FirErrorExpression, data: Nothing?): ConstantArgumentKind {
        // Error expression already signalizes about some problem, and later we will report some diagnostic.
        // Depending on the context, we can count this as valid or as error expression.
        // So we delegate the final decision to the caller.
        return ConstantArgumentKind.RESOLUTION_ERROR
    }

    override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: Nothing?): ConstantArgumentKind {
        return namedArgumentExpression.expression.accept(this, data)
    }

    override fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: Nothing?): ConstantArgumentKind {
        return spreadArgumentExpression.expression.accept(this, data)
    }

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Nothing?): ConstantArgumentKind {
        if (typeOperatorCall.operation != FirOperation.AS) {
            return ConstantArgumentKind.NOT_CONST
        }

        if (!typeOperatorCall.argument.resolvedType.isSubtypeOf(typeOperatorCall.resolvedType, session)) {
            return ConstantArgumentKind.NOT_CONST
        }

        return typeOperatorCall.argument.accept(this, data)
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: Nothing?): ConstantArgumentKind {
        if (!whenExpression.isProperlyExhaustive || !intrinsicConstEvaluation) {
            return ConstantArgumentKind.NOT_CONST
        }

        whenExpression.subjectVariable?.initializer?.accept(this, data)?.ifNotValidConst { return it }
        for (branch in whenExpression.branches) {
            when (branch.condition) {
                is FirElseIfTrueCondition -> { /* skip */ }
                else -> branch.condition.accept(this, data).ifNotValidConst { return it }
            }
            branch.result.statements.forEach { stmt ->
                if (stmt !is FirExpression) return ConstantArgumentKind.NOT_CONST
                stmt.accept(this, data).ifNotValidConst { return it }
            }
        }
        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: Nothing?): ConstantArgumentKind {
        return if (intrinsicConstEvaluation) ConstantArgumentKind.VALID_CONST else ConstantArgumentKind.NOT_CONST
    }

    override fun visitLiteralExpression(literalExpression: FirLiteralExpression, data: Nothing?): ConstantArgumentKind {
        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: Nothing?): ConstantArgumentKind {
        return comparisonExpression.compareToCall.accept(this, data)
    }

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: Nothing?): ConstantArgumentKind {
        for (exp in stringConcatenationCall.arguments) {
            if (exp is FirLiteralExpression && exp.value == null) {
                // `null` is allowed
                continue
            }

            if (!exp.hasAllowedCompileTimeType()) {
                return ConstantArgumentKind.NOT_CONST
            }
            exp.accept(this, data).ifNotValidConst { return it }
        }
        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: Nothing?): ConstantArgumentKind {
        if (equalityOperatorCall.operation == FirOperation.IDENTITY || equalityOperatorCall.operation == FirOperation.NOT_IDENTITY) {
            return ConstantArgumentKind.NOT_CONST
        }

        for (exp in equalityOperatorCall.arguments) {
            if (exp is FirLiteralExpression && exp.value == null) {
                return ConstantArgumentKind.NOT_CONST
            }

            if (!exp.hasAllowedCompileTimeType() || exp.getExpandedType().isUnsignedType) {
                return ConstantArgumentKind.NOT_CONST
            }

            exp.accept(this, data).ifNotValidConst { return it }
        }

        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitBooleanOperatorExpression(booleanOperatorExpression: FirBooleanOperatorExpression, data: Nothing?): ConstantArgumentKind {
        if (!booleanOperatorExpression.leftOperand.resolvedType.isBoolean || !booleanOperatorExpression.rightOperand.resolvedType.isBoolean) {
            return ConstantArgumentKind.NOT_CONST
        }

        booleanOperatorExpression.leftOperand.accept(this, data).ifNotValidConst { return it }
        booleanOperatorExpression.rightOperand.accept(this, data).ifNotValidConst { return it }
        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: Nothing?): ConstantArgumentKind {
        var coneType = getClassCall.argument.getExpandedType()

        if (coneType is ConeErrorType)
            return ConstantArgumentKind.NOT_CONST

        while (coneType.classId == StandardClassIds.Array)
            coneType = (coneType.lowerBoundIfFlexible().typeArguments.first() as? ConeKotlinTypeProjection)?.type ?: break

        val argument = getClassCall.argument
        return when {
            coneType is ConeTypeParameterType -> ConstantArgumentKind.KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR
            argument is FirResolvedQualifier -> ConstantArgumentKind.VALID_CONST
            argument is FirClassReferenceExpression -> ConstantArgumentKind.VALID_CONST
            else -> ConstantArgumentKind.NOT_KCLASS_LITERAL
        }
    }

    override fun visitVarargArgumentsExpression(
        varargArgumentsExpression: FirVarargArgumentsExpression, data: Nothing?
    ): ConstantArgumentKind {
        for (exp in varargArgumentsExpression.arguments) {
            exp.accept(this, data).ifNotValidConst { return it }
        }
        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitArrayLiteral(arrayLiteral: FirArrayLiteral, data: Nothing?): ConstantArgumentKind {
        for (exp in arrayLiteral.arguments) {
            exp.accept(this, data).ifNotValidConst { return it }
        }
        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: Nothing?): ConstantArgumentKind {
        val classSymbol = thisReceiverExpression.calleeReference.boundSymbol as? FirClassSymbol
        return if (classSymbol?.classKind == ClassKind.OBJECT) {
            ConstantArgumentKind.VALID_CONST
        } else {
            ConstantArgumentKind.NOT_CONST
        }
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression, data: Nothing?
    ): ConstantArgumentKind {
        val propertySymbol = propertyAccessExpression.calleeReference.toResolvedCallableSymbol()
        if (propertySymbol in propertyStack.get()) return ConstantArgumentKind.NOT_CONST
        when (propertySymbol) {
            // Null symbol means some error occurred.
            // We use the same logic as in `visitErrorExpression`.
            // Better to report "UNRESOLVED_REFERENCE" later than some "NOT_CONST" diagnostic right now.
            null -> return ConstantArgumentKind.RESOLUTION_ERROR
            is FirPropertySymbol -> {
                val classKindOfParent = (propertySymbol.getReferencedClassSymbol() as? FirRegularClassSymbol)?.classKind
                if (classKindOfParent == ClassKind.ENUM_CLASS) {
                    return ConstantArgumentKind.ENUM_NOT_CONST
                }

                val isConstWithoutInitializer = propertySymbol.unwrapFakeOverrides().canBeEvaluated()
                        || propertySymbol.isCompileTimeBuiltinProperty()
                when {
                    propertySymbol.isConst || isConstWithoutInitializer -> {
                        val receivers = listOf(propertyAccessExpression.dispatchReceiver, propertyAccessExpression.extensionReceiver)
                        if (receivers.count { it != null } == 2) {
                            return ConstantArgumentKind.NOT_CONST
                        }
                        receivers.singleOrNull { it != null }?.accept(this, data)?.ifNotValidConst { return it }
                    }
                    propertySymbol.isLocal -> return ConstantArgumentKind.NOT_CONST
                    propertyAccessExpression.getExpandedType().classId == StandardClassIds.KClass -> return ConstantArgumentKind.NOT_KCLASS_LITERAL
                }

                return when {
                    isConstWithoutInitializer -> ConstantArgumentKind.VALID_CONST
                    propertySymbol.isConst -> {
                        // even if called on CONSTANT_EVALUATION, it's safe to call resolvedInitializer, as intializers of const vals
                        // are resolved at previous IMPLICIT_TYPES_BODY_RESOLVE phase
                        val initializer = propertySymbol.resolvedInitializer
                        propertySymbol.visit { initializer?.accept(this, data) } ?: ConstantArgumentKind.RESOLUTION_ERROR
                    }

                    // if it called at checkers stage it's safe to call resolvedInitializer
                    // even if it will trigger BODY_RESOLVE phase, we don't violate phase contracts
                    calledOnCheckerStage -> when (propertySymbol.resolvedInitializer) {
                        is FirLiteralExpression -> when {
                            propertySymbol.isVal -> ConstantArgumentKind.NOT_CONST_VAL_IN_CONST_EXPRESSION
                            else -> ConstantArgumentKind.NOT_CONST
                        }
                        is FirGetClassCall -> ConstantArgumentKind.NOT_KCLASS_LITERAL
                        else -> ConstantArgumentKind.NOT_CONST
                    }

                    else -> ConstantArgumentKind.NOT_CONST
                }
            }
            is FirFieldSymbol -> {
                if (propertySymbol.isStatic && propertySymbol.modality == Modality.FINAL && propertySymbol.hasConstantInitializer) {
                    return ConstantArgumentKind.VALID_CONST
                }
            }
            is FirEnumEntrySymbol -> return ConstantArgumentKind.VALID_CONST
        }

        return ConstantArgumentKind.NOT_CONST
    }

    override fun visitIntegerLiteralOperatorCall(
        integerLiteralOperatorCall: FirIntegerLiteralOperatorCall, data: Nothing?
    ): ConstantArgumentKind {
        return visitFunctionCall(integerLiteralOperatorCall, data)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: Nothing?): ConstantArgumentKind {
        val calleeReference = functionCall.calleeReference
        if (calleeReference is FirErrorNamedReference || calleeReference is FirResolvedErrorReference) {
            return ConstantArgumentKind.RESOLUTION_ERROR
        }
        if (functionCall.getExpandedType().classId == StandardClassIds.KClass) {
            return ConstantArgumentKind.NOT_KCLASS_LITERAL
        }

        if (calleeReference !is FirResolvedNamedReference) return ConstantArgumentKind.NOT_CONST
        return when (val symbol = calleeReference.resolvedSymbol) {
            is FirNamedFunctionSymbol -> visitNamedFunction(functionCall, symbol)
            is FirConstructorSymbol -> visitConstructorCall(functionCall, symbol)
            else -> ConstantArgumentKind.NOT_CONST
        }
    }

    private fun visitNamedFunction(functionCall: FirFunctionCall, symbol: FirNamedFunctionSymbol): ConstantArgumentKind {
        if (!symbol.canBeEvaluated() && !functionCall.isCompileTimeBuiltinCall()) {
            return ConstantArgumentKind.NOT_CONST
        }

        for (exp in functionCall.arguments.plus(functionCall.dispatchReceiver).plus(functionCall.extensionReceiver)) {
            if (exp == null) continue
            if (!exp.hasAllowedCompileTimeType()) return ConstantArgumentKind.NOT_CONST

            exp.accept(this, null).ifNotValidConst { return it }
        }

        return ConstantArgumentKind.VALID_CONST
    }

    private fun visitConstructorCall(constructorCall: FirFunctionCall, symbol: FirConstructorSymbol): ConstantArgumentKind {
        val classKindOfParent = (symbol.getReferencedClassSymbol() as? FirClassLikeSymbol<*>)?.fullyExpandedClass(session)?.classKind
        return when {
            classKindOfParent == ClassKind.ANNOTATION_CLASS -> ConstantArgumentKind.VALID_CONST
            constructorCall.getExpandedType().isUnsignedType -> {
                constructorCall.arguments.forEach { argumentExpression ->
                    argumentExpression.accept(this, null).ifNotValidConst { return it }
                }
                ConstantArgumentKind.VALID_CONST
            }
            else -> ConstantArgumentKind.NOT_CONST
        }
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression, data: Nothing?
    ): ConstantArgumentKind {
        val expressionType = qualifiedAccessExpression.getExpandedType()
        if (expressionType.isReflectFunctionType(session) || expressionType.isKProperty(session) || expressionType.isKMutableProperty(session)) {
            return qualifiedAccessExpression.dispatchReceiver?.accept(this, data) ?: ConstantArgumentKind.VALID_CONST
        }

        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: Nothing?): ConstantArgumentKind {
        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier, data: Nothing?): ConstantArgumentKind {
        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: Nothing?): ConstantArgumentKind {
        return visitQualifiedAccessExpression(callableReferenceAccess, data)
    }

    override fun visitEnumEntryDeserializedAccessExpression(
        enumEntryDeserializedAccessExpression: FirEnumEntryDeserializedAccessExpression,
        data: Nothing?,
    ): ConstantArgumentKind {
        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitClassReferenceExpression(
        classReferenceExpression: FirClassReferenceExpression,
        data: Nothing?,
    ): ConstantArgumentKind {
        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: Nothing?): ConstantArgumentKind {
        return visitAnnotation(annotationCall, data)
    }

    override fun visitAnnotation(annotation: FirAnnotation, data: Nothing?): ConstantArgumentKind {
        for (argument in annotation.argumentMapping.mapping.values) {
            val argumentKind = argument.accept(this, data)
            if (argumentKind != ConstantArgumentKind.VALID_CONST) return argumentKind
        }
        return ConstantArgumentKind.VALID_CONST
    }

    // --- Utils ---
    private fun FirBasedSymbol<*>.canBeEvaluated(): Boolean {
        return intrinsicConstEvaluation && this.hasAnnotation(StandardClassIds.Annotations.IntrinsicConstEvaluation, session)
    }

    private fun FirExpression.hasAllowedCompileTimeType(): Boolean {
        // See visitErrorExpression for details. Here we count the type as valid and take a decision later.
        if (this is FirErrorExpression) return true

        val expClassId = resolvedType.unwrapToSimpleTypeUsingLowerBound().fullyExpandedType(session).classId
        // TODO, KT-59823: add annotation for allowed constant types
        return expClassId in StandardClassIds.constantAllowedTypes
    }

    private fun FirExpression.getExpandedType() = resolvedType.fullyExpandedType(session)

    private fun FirFunctionCall.isCompileTimeBuiltinCall(): Boolean {
        val calleeReference = this.calleeReference
        if (calleeReference !is FirResolvedNamedReference) return false

        val name = calleeReference.name
        val symbol = calleeReference.resolvedSymbol as? FirCallableSymbol
        if (!symbol.fromKotlin()) return false

        val receiverClassId = this.dispatchReceiver?.getExpandedType()?.classId

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

    private fun FirPropertySymbol.isCompileTimeBuiltinProperty(): Boolean {
        val receiverType = dispatchReceiverType ?: resolvedReceiverTypeRef?.coneTypeSafe<ConeKotlinType>() ?: return false
        val receiverClassId = receiverType.fullyExpandedType(session).classId ?: return false
        return when (name.asString()) {
            "length" -> receiverClassId == StandardClassIds.String
            "code" -> receiverClassId == StandardClassIds.Char
            else -> false
        }
    }

    private fun FirCallableSymbol<*>?.fromKotlin(): Boolean {
        return this?.callableId?.packageName?.asString() == "kotlin"
    }

    private fun FirCallableSymbol<*>?.getReferencedClassSymbol(): FirBasedSymbol<*>? =
        this?.resolvedReturnTypeRef?.coneType?.toSymbol(session)
}
