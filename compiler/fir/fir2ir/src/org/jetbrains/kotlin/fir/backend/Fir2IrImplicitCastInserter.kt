/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

class Fir2IrImplicitCastInserter(
    private val components: Fir2IrComponents
) : Fir2IrComponents by components, FirDefaultVisitor<IrElement, IrElement>() {

    private fun ConeKotlinType.toIrType(typeOrigin: ConversionTypeOrigin): IrType = with(typeConverter) {
        toIrType(typeOrigin)
    }

    override fun visitElement(element: FirElement, data: IrElement): IrElement {
        TODO("Should not be here: ${element::class}: ${element.render()}")
    }

    override fun visitAnnotation(annotation: FirAnnotation, data: IrElement): IrElement = data

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: IrElement): IrElement = data

    override fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression, data: IrElement): IrElement = data

    override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression, data: IrElement): IrElement {
        return data
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: IrElement): IrElement = data

    // TODO: maybe a place to do coerceIntToAnotherIntegerType?
    override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: IrElement): IrElement = data

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: IrElement): IrElement = data

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: IrElement): IrElement = data

    override fun <T> visitLiteralExpression(literalExpression: FirLiteralExpression<T>, data: IrElement): IrElement = data

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: IrElement): IrElement = data

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: IrElement): IrElement = data

    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: IrElement): IrElement = data

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: IrElement): IrElement = data

    override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier, data: IrElement): IrElement {
        // Support for error suppression case
        return visitResolvedQualifier(errorResolvedQualifier, data)
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: IrElement): IrElement = data

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: IrElement): IrElement = data

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: IrElement): IrElement = data

    override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject, data: IrElement): IrElement = data

    override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: IrElement): IrElement = data

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: IrElement): IrElement = data

    // TODO: element-wise cast?
    override fun visitArrayLiteral(arrayLiteral: FirArrayLiteral, data: IrElement): IrElement = data

    // TODO: something to do w.r.t. SAM?
    override fun visitLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression, data: IrElement): IrElement = data

    // TODO: element-wise cast?
    override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: IrElement): IrElement = data

    // TODO: element-wise cast?
    override fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression, data: IrElement): IrElement = data

    // TODO: element-wise cast?
    override fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: IrElement): IrElement = data

    // ==================================================================================

    override fun visitExpression(expression: FirExpression, data: IrElement): IrElement {
        return when (expression) {
            is FirBlock -> (data as IrContainerExpression).insertImplicitCasts()
            is FirUnitExpression -> coerceToUnitIfNeeded(data as IrExpression, irBuiltIns)
            else -> data
        }
    }

    override fun visitStatement(statement: FirStatement, data: IrElement): IrElement {
        return when (statement) {
            is FirTypeAlias -> data
            is FirUnitExpression -> coerceToUnitIfNeeded(data as IrExpression, irBuiltIns)
            is FirBlock -> (data as IrContainerExpression).insertImplicitCasts()
            else -> statement.accept(this, data)
        }
    }

    // ==================================================================================

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: IrElement): IrElement {
        if (data is IrBlock) {
            return data.insertImplicitCasts()
        }
        val irWhen = data as IrWhen
        if (irWhen.branches.size != whenExpression.branches.size) {
            return data
        }
        val firBranchMap = irWhen.branches.zip(whenExpression.branches).toMap()
        irWhen.branches.replaceAll {
            visitWhenBranch(firBranchMap.getValue(it), it)
        }
        return data
    }

    override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: IrElement): IrElement = data

    override fun visitWhenBranch(whenBranch: FirWhenBranch, data: IrElement): IrBranch {
        val irBranch = data as IrBranch
        (irBranch.result as? IrContainerExpression)?.insertImplicitCasts()
        return data
    }

    // TODO: Need to visit lhs/rhs branches?
    override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: IrElement): IrElement = data

    // ==================================================================================

    override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: IrElement): IrElement {
        val loop = data as IrDoWhileLoop
        (loop.body as? IrContainerExpression)?.insertImplicitCasts(coerceLastExpressionToUnit = true)
        return data
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: IrElement): IrElement {
        val loop = data as IrWhileLoop
        (loop.body as? IrContainerExpression)?.insertImplicitCasts(coerceLastExpressionToUnit = true)
        return data
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression, data: IrElement): IrElement = data

    override fun visitContinueExpression(continueExpression: FirContinueExpression, data: IrElement): IrElement = data

    // ==================================================================================

    override fun visitTryExpression(tryExpression: FirTryExpression, data: IrElement): IrElement {
        val irTry = data as IrTry

        (irTry.tryResult as? IrContainerExpression)?.insertImplicitCasts()
        for (catch in irTry.catches) {
            (catch.result as? IrContainerExpression)?.insertImplicitCasts()
        }
        (irTry.finallyExpression as? IrContainerExpression)?.insertImplicitCasts(coerceLastExpressionToUnit = true)
        return data
    }

    override fun visitThrowExpression(throwExpression: FirThrowExpression, data: IrElement): IrElement =
        (data as IrThrow).insertSpecialCast(throwExpression, throwExpression.exception.resolvedType, throwExpression.resolvedType)

    override fun visitBlock(block: FirBlock, data: IrElement): IrElement =
        (data as? IrContainerExpression)?.insertImplicitCasts() ?: data

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: IrElement): IrElement {
        val irReturn = data as? IrReturn ?: return data
        val expectedType = returnExpression.target.labeledElement.returnTypeRef
        irReturn.value = irReturn.value.insertSpecialCast(returnExpression.result, returnExpression.result.resolvedType, expectedType.coneType)
        return data
    }

    // ==================================================================================

    /**
     * This functions processes the following casts:
     * - coercion to Unit
     * - nullability casts based on nullability annotations
     * - casts for dynamic types
     */
    internal fun IrExpression.insertSpecialCast(
        expression: FirExpression,
        valueType: ConeKotlinType,
        expectedType: ConeKotlinType,
    ): IrExpression {
        if (this is IrTypeOperatorCall) {
            return this
        }

        if (this is IrContainerExpression) {
            insertImplicitCasts(coerceLastExpressionToUnit = type.isUnit())
        }

        val expandedValueType = valueType.fullyExpandedType(session)
        val expandedExpectedType = expectedType.fullyExpandedType(session)

        return when {
            expandedExpectedType.isUnit -> {
                coerceToUnitIfNeeded(this, irBuiltIns)
            }
            expandedValueType is ConeDynamicType -> {
                if (expandedExpectedType !is ConeDynamicType && !expandedExpectedType.isNullableAny) {
                    implicitCast(this, expandedExpectedType.toIrType(ConversionTypeOrigin.DEFAULT))
                } else {
                    this
                }
            }
            typeCanBeEnhancedOrFlexibleNullable(expandedValueType, session) && !expandedExpectedType.acceptsNullValues() -> {
                insertImplicitNotNullCastIfNeeded(expression)
            }
            // TODO: coerceIntToAnotherIntegerType
            // TODO: even implicitCast call can be here?
            else -> this
        }
    }

    internal fun IrExpression.insertCastForSmartcastWithIntersection(
        argumentType: ConeKotlinType,
        expectedType: ConeKotlinType
    ): IrExpression {
        if (argumentType !is ConeIntersectionType) return this
        val approximatedArgumentType = argumentType.approximateForIrOrNull() ?: argumentType
        if (approximatedArgumentType.isSubtypeOf(expectedType, session)) return this

        return findComponentOfIntersectionForExpectedType(argumentType, expectedType)?.let {
            implicitCast(this, it.toIrType())
        } ?: this
    }

    private fun ConeKotlinType.acceptsNullValues(): Boolean {
        if (this is ConeCapturedType && this.constructor.projection.kind == ProjectionKind.IN) {
            return constructor.projection.type!!.canBeNull(session)
        }
        return canBeNull(session) || hasEnhancedNullability
    }

    private fun IrExpression.insertImplicitNotNullCastIfNeeded(expression: FirExpression): IrExpression {
        if (this is IrGetEnumValue) return this
        // [TypeOperatorLowering] will retrieve the source (from start offset to end offset) as an assertion message.
        // Avoid type casting if we can't determine the source for some reasons, e.g., implicit `this` receiver.
        if (expression.source == null) return this
        return implicitNotNullCast(this)
    }

    private fun IrContainerExpression.insertImplicitCasts(coerceLastExpressionToUnit: Boolean = false): IrContainerExpression {
        if (statements.isEmpty()) return this

        val lastIndex = statements.lastIndex
        statements.forEachIndexed { i, irStatement ->
            if (irStatement !is IrErrorCallExpression && irStatement is IrExpression) {
                if (i != lastIndex || coerceLastExpressionToUnit) {
                    statements[i] = coerceToUnitIfNeeded(irStatement, irBuiltIns)
                }
                // TODO: for the last statement, need to cast to the return type if mismatched
            }
        }

        return this
    }

    internal fun IrBlockBody.insertImplicitCasts(): IrBlockBody {
        if (statements.isEmpty()) return this

        statements.forEachIndexed { i, irStatement ->
            if (irStatement !is IrErrorCallExpression && irStatement is IrExpression) {
                statements[i] = coerceToUnitIfNeeded(irStatement, irBuiltIns)
            }
        }
        return this
    }

    override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: IrElement): IrElement {
        // We don't want an implicit cast to Nothing?. This expression just encompasses nullability after null check.
        return if (smartCastExpression.isStable && smartCastExpression.smartcastTypeWithoutNullableNothing == null) {
            val smartcastedType = smartCastExpression.resolvedType
            val approximatedType = smartcastedType.approximateForIrOrNull()
            if (approximatedType != null) {
                if (smartCastExpression.originalExpression.resolvedType.isSubtypeOf(approximatedType, session)) {
                    return data
                }
            }
            implicitCastOrExpression(data as IrExpression, approximatedType ?: smartcastedType)
        } else {
            data
        }
    }

    internal fun implicitCastFromReceivers(
        originalIrReceiver: IrExpression,
        receiver: FirExpression,
        selector: FirQualifiedAccessExpression,
        typeOrigin: ConversionTypeOrigin,
    ): IrExpression {
        return implicitCastFromReceiverForIntersectionTypeOrNull(
            originalIrReceiver,
            receiver,
            selector,
            typeOrigin
        ) ?: implicitCastOrExpression(originalIrReceiver, receiver.resolvedType, typeOrigin)
    }

    private fun implicitCastFromReceiverForIntersectionTypeOrNull(
        originalIrReceiver: IrExpression,
        receiver: FirExpression,
        selector: FirQualifiedAccessExpression,
        typeOrigin: ConversionTypeOrigin,
    ): IrExpression? {
        val receiverExpressionType = receiver.resolvedType as? ConeIntersectionType ?: return null
        val referencedDeclaration = selector.calleeReference.toResolvedCallableSymbol()?.unwrapCallRepresentative()?.fir

        val receiverType = with(selector) {
            when {
                receiver === dispatchReceiver -> {
                    val dispatchReceiverType = referencedDeclaration?.dispatchReceiverType as? ConeClassLikeType ?: return null
                    dispatchReceiverType.replaceArgumentsWithStarProjections()
                }
                receiver === extensionReceiver -> {
                    val extensionReceiverType = referencedDeclaration?.receiverParameter?.typeRef?.coneType ?: return null
                    val substitutor = selector.buildSubstitutorByCalledCallable()
                    val substitutedType = substitutor.substituteOrSelf(extensionReceiverType)
                    // Frontend may write captured types as type arguments (by design), so we need to approximate receiver type after substitution
                    val approximatedType = session.typeApproximator.approximateToSuperType(
                        substitutedType,
                        TypeApproximatorConfiguration.InternalTypesApproximation
                    )
                    approximatedType ?: substitutedType
                }
                else -> return null
            }
        }

        return findComponentOfIntersectionForExpectedType(receiverExpressionType, receiverType)?.let {
            implicitCastOrExpression(originalIrReceiver, it, typeOrigin)
        }
    }

    private fun findComponentOfIntersectionForExpectedType(type: ConeIntersectionType, expectedType: ConeKotlinType): ConeKotlinType? {
        for (componentType in type.intersectedTypes) {
            if (AbstractTypeChecker.isSubtypeOf(session.typeContext, componentType, expectedType)) {
                return componentType
            }
        }
        return null
    }

    private fun implicitCastOrExpression(
        original: IrExpression, castType: ConeKotlinType, typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT
    ): IrExpression {
        return implicitCastOrExpression(original, castType.toIrType(typeOrigin))
    }

    companion object {
        internal fun implicitCastOrExpression(original: IrExpression, castType: IrType): IrExpression {
            val originalNotNull = original.type.makeNotNull()
            if (originalNotNull == castType.makeNotNull()) return original
            return implicitCast(original, castType)
        }

        private fun implicitCast(original: IrExpression, castType: IrType): IrExpression {
            val typeOperator = if (original.type is IrDynamicType) {
                IrTypeOperator.IMPLICIT_DYNAMIC_CAST
            } else {
                IrTypeOperator.IMPLICIT_CAST
            }

            return implicitCast(original, castType, typeOperator)
        }

        internal fun coerceToUnitIfNeeded(original: IrExpression, irBuiltIns: IrBuiltIns): IrExpression {
            val valueType = original.type
            return if (valueType.isUnit() || valueType.isNothing())
                original
            else
                IrTypeOperatorCallImpl(
                    original.startOffset, original.endOffset,
                    irBuiltIns.unitType,
                    IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
                    irBuiltIns.unitType,
                    original
                )
        }

        internal fun implicitNotNullCast(original: IrExpression): IrTypeOperatorCall {
            // Cast type massage 1. Remove @EnhancedNullability
            // Cast type massage 2. Convert it to a non-null variant (in case of @FlexibleNullability)
            val castType = original.type.removeAnnotations { annotationCall ->
                val constructorSymbol = annotationCall.symbol.takeIf { it.isBound } ?: return@removeAnnotations false
                /*
                 * @EnhancedNullability and @FlexibleNullability are symbols from builtins and should be already
                 *   bound at the time of body conversion, so it's safe to take the owner for them
                 * If symbol is unbound then this annotation can not be neither @EnhancedNullability or @FlexibleNullability
                 */
                @OptIn(UnsafeDuringIrConstructionAPI::class)
                val classId = constructorSymbol.owner.parentAsClass.classId
                classId == StandardClassIds.Annotations.EnhancedNullability ||
                        classId == StandardClassIds.Annotations.FlexibleNullability
            }.makeNotNull()
            return IrTypeOperatorCallImpl(
                original.startOffset,
                original.endOffset,
                castType,
                IrTypeOperator.IMPLICIT_NOTNULL,
                castType,
                original
            )
        }

        internal fun typeCanBeEnhancedOrFlexibleNullable(type: ConeKotlinType, session: FirSession): Boolean {
            return when {
                type.hasEnhancedNullability -> true
                type.hasFlexibleNullability && type.canBeNull(session) -> true
                else -> false
            }
        }

        private val ConeKotlinType.hasFlexibleNullability: Boolean
            get() {
                if (this !is ConeFlexibleType) return false
                return lowerBound.isMarkedNullable != upperBound.isMarkedNullable
            }
    }
}
