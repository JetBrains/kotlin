/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

fun ConeKotlinType.canBeUsedForConstVal(): Boolean = with(lowerBoundIfFlexible()) { isPrimitive || isString || isUnsignedType }

fun canBeEvaluatedAtCompileTime(expression: FirExpression?, session: FirSession): Boolean {
    return checkConstantArguments(expression, session) == ConstantArgumentKind.VALID_CONST
}

internal fun checkConstantArguments(
    expression: FirExpression?,
    session: FirSession,
): ConstantArgumentKind {
    if (expression == null) return ConstantArgumentKind.VALID_CONST
    return expression.accept(FirConstCheckVisitor(session), null)
}

internal enum class ConstantArgumentKind {
    VALID_CONST,
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

private class FirConstCheckVisitor(private val session: FirSession) : FirVisitor<ConstantArgumentKind, Nothing?>() {
    private val intrinsicConstEvaluation = session.languageVersionSettings.supportsFeature(LanguageFeature.IntrinsicConstEvaluation)

    private val compileTimeFunctions = setOf(
        *OperatorNameConventions.BINARY_OPERATION_NAMES.toTypedArray(), *OperatorNameConventions.UNARY_OPERATION_NAMES.toTypedArray(),
        OperatorNameConventions.SHL, OperatorNameConventions.SHR, OperatorNameConventions.USHR,
        OperatorNameConventions.OR, OperatorNameConventions.AND, OperatorNameConventions.XOR,
        OperatorNameConventions.COMPARE_TO
    )

    private val compileTimeExtensionFunctions = listOf("floorDiv", "mod", "code").mapTo(hashSetOf()) { Name.identifier(it) }

    private val compileTimeConversionFunctions = listOf(
        "toInt", "toLong", "toShort", "toByte", "toFloat", "toDouble", "toChar", "toBoolean"
    ).mapTo(hashSetOf()) { Name.identifier(it) }

    override fun visitElement(element: FirElement, data: Nothing?): ConstantArgumentKind {
        return ConstantArgumentKind.NOT_CONST
    }

    override fun visitErrorExpression(errorExpression: FirErrorExpression, data: Nothing?): ConstantArgumentKind {
        // Error expression already signalizes about some problem, and later we will report some diagnostic.
        // We count this expression as valid and skip diagnostic reporting not to pollute output with a lot of error messages.
        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: Nothing?): ConstantArgumentKind {
        return namedArgumentExpression.expression.accept(this, data)
    }

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Nothing?): ConstantArgumentKind {
        return if (typeOperatorCall.operation == FirOperation.AS) ConstantArgumentKind.NOT_CONST else ConstantArgumentKind.VALID_CONST
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: Nothing?): ConstantArgumentKind {
        if (!whenExpression.isProperlyExhaustive || !intrinsicConstEvaluation) {
            return ConstantArgumentKind.NOT_CONST
        }

        whenExpression.subject?.accept(this, data)?.ifNotValidConst { return it }
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

    override fun <T> visitLiteralExpression(literalExpression: FirLiteralExpression<T>, data: Nothing?): ConstantArgumentKind {
        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: Nothing?): ConstantArgumentKind {
        return comparisonExpression.compareToCall.accept(this, data)
    }

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: Nothing?): ConstantArgumentKind {
        for (exp in stringConcatenationCall.arguments) {
            if (exp is FirResolvedQualifier || exp is FirGetClassCall) {
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
            if (exp is FirLiteralExpression<*> && exp.value == null) {
                return ConstantArgumentKind.NOT_CONST
            }

            if (exp is FirResolvedQualifier || exp is FirGetClassCall || exp.getExpandedType().isUnsignedType) {
                return ConstantArgumentKind.NOT_CONST
            }

            exp.accept(this, data).ifNotValidConst { return it }
        }

        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: Nothing?): ConstantArgumentKind {
        binaryLogicExpression.leftOperand.accept(this, data).ifNotValidConst { return it }
        binaryLogicExpression.rightOperand.accept(this, data).ifNotValidConst { return it }
        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: Nothing?): ConstantArgumentKind {
        var coneType = getClassCall.argument.getExpandedType()

        if (coneType is ConeErrorType)
            return ConstantArgumentKind.NOT_CONST

        while (coneType.classId == StandardClassIds.Array)
            coneType = (coneType.lowerBoundIfFlexible().typeArguments.first() as? ConeKotlinTypeProjection)?.type ?: break

        return when {
            coneType is ConeTypeParameterType -> ConstantArgumentKind.KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR
            getClassCall.argument !is FirResolvedQualifier -> ConstantArgumentKind.NOT_KCLASS_LITERAL
            else -> ConstantArgumentKind.VALID_CONST
        }
    }

    override fun visitArrayLiteral(arrayLiteral: FirArrayLiteral, data: Nothing?): ConstantArgumentKind {
        for (exp in arrayLiteral.arguments) {
            exp.accept(this, data).ifNotValidConst { return it }
        }
        return ConstantArgumentKind.VALID_CONST
    }

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: Nothing?): ConstantArgumentKind {
        return ConstantArgumentKind.NOT_CONST
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression, data: Nothing?
    ): ConstantArgumentKind {
        val propertySymbol = propertyAccessExpression.toReference(session)?.toResolvedCallableSymbol(discardErrorReference = true)
        when (propertySymbol) {
            // Null symbol means some error occurred.
            // We use the same logic as in `visitErrorExpression`.
            // Better to report "UNRESOLVED_REFERENCE" later than some "NOT_CONST" diagnostic right now.
            null -> return ConstantArgumentKind.VALID_CONST
            is FirPropertySymbol -> {
                val classKindOfParent = (propertySymbol.getReferencedClassSymbol() as? FirRegularClassSymbol)?.classKind
                if (classKindOfParent == ClassKind.ENUM_CLASS) {
                    return ConstantArgumentKind.ENUM_NOT_CONST
                }

                when {
                    propertySymbol.unwrapFakeOverrides().canBeEvaluated() || propertySymbol.isCompileTimeBuiltinProperty() -> {
                        val receiver = listOf(propertyAccessExpression.dispatchReceiver, propertyAccessExpression.extensionReceiver)
                            .single { it != null }
                        return receiver?.accept(this, data) ?: ConstantArgumentKind.VALID_CONST
                    }
                    propertySymbol.isLocal -> return ConstantArgumentKind.NOT_CONST
                    propertyAccessExpression.getExpandedType().classId == StandardClassIds.KClass -> return ConstantArgumentKind.NOT_KCLASS_LITERAL
                    propertySymbol.isConst -> return ConstantArgumentKind.VALID_CONST
                }

                // Ok, because we only look at the structure, not resolution-dependent properties.
                @OptIn(SymbolInternals::class)
                return when (propertySymbol.fir.initializer) {
                    is FirLiteralExpression<*> -> when {
                        propertySymbol.isVal -> ConstantArgumentKind.NOT_CONST_VAL_IN_CONST_EXPRESSION
                        else -> ConstantArgumentKind.NOT_CONST
                    }
                    is FirGetClassCall -> ConstantArgumentKind.NOT_KCLASS_LITERAL
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
            return ConstantArgumentKind.VALID_CONST
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
            val expClassId = exp.getExpandedType().lowerBoundIfFlexible().fullyExpandedClassId(session)
            // TODO, KT-59823: add annotation for allowed constant types
            if (expClassId !in StandardClassIds.constantAllowedTypes) {
                return ConstantArgumentKind.NOT_CONST
            }

            exp.accept(this, null).ifNotValidConst { return it }
        }

        return ConstantArgumentKind.VALID_CONST
    }

    private fun visitConstructorCall(constructorCall: FirFunctionCall, symbol: FirConstructorSymbol): ConstantArgumentKind {
        val classKindOfParent = (symbol.getReferencedClassSymbol() as? FirRegularClassSymbol)?.classKind
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

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: Nothing?): ConstantArgumentKind {
        return visitQualifiedAccessExpression(callableReferenceAccess, data)
    }

    // --- Utils ---
    private fun FirBasedSymbol<*>.canBeEvaluated(): Boolean {
        return intrinsicConstEvaluation && this.hasAnnotation(StandardClassIds.Annotations.IntrinsicConstEvaluation, session)
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
            name in compileTimeConversionFunctions
        ) return true

        if (calleeReference.name == OperatorNameConventions.GET && receiverClassId == StandardClassIds.String) return true

        return false
    }

    private fun FirPropertySymbol.isCompileTimeBuiltinProperty(): Boolean {
        val receiverType = dispatchReceiverType ?: receiverParameter?.typeRef?.coneTypeSafe<ConeKotlinType>() ?: return false
        val receiverClassId = receiverType.fullyExpandedClassId(session) ?: return false
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
        this?.resolvedReturnTypeRef
            ?.coneTypeSafe<ConeLookupTagBasedType>()
            ?.lookupTag
            ?.toSymbol(session)
}
