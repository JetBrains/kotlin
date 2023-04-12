/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirStubStatement
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractTypeChecker

class Fir2IrImplicitCastInserter(
    private val components: Fir2IrComponents
) : Fir2IrComponents by components, FirDefaultVisitor<IrElement, IrElement>() {

    private fun FirTypeRef.toIrType(conversionTypeContext: ConversionTypeContext): IrType = with(typeConverter) {
        toIrType(conversionTypeContext)
    }

    private fun ConeKotlinType.toIrType(conversionTypeContext: ConversionTypeContext): IrType = with(typeConverter) {
        toIrType(conversionTypeContext)
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

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: IrElement): IrElement = data

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: IrElement): IrElement = data

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: IrElement): IrElement = data

    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: IrElement): IrElement = data

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: IrElement): IrElement = data

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: IrElement): IrElement = data

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: IrElement): IrElement = data

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: IrElement): IrElement = data

    override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject, data: IrElement): IrElement = data

    override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: IrElement): IrElement = data

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: IrElement): IrElement = data

    // TODO: element-wise cast?
    override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall, data: IrElement): IrElement = data

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
            FirStubStatement -> data
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

    // TODO: cast `condition` expression to boolean?
    override fun visitWhenBranch(whenBranch: FirWhenBranch, data: IrElement): IrBranch {
        val irBranch = data as IrBranch
        (irBranch.result as? IrContainerExpression)?.let {
            irBranch.result = it.insertImplicitCasts()
        }
        return data
    }

    // TODO: Need to visit lhs/rhs branches?
    override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: IrElement): IrElement = data

    // ==================================================================================

    // TODO: cast `condition` expression to boolean?
    override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: IrElement): IrElement {
        val loop = data as IrDoWhileLoop
        (loop.body as? IrContainerExpression)?.let {
            loop.body = it.insertImplicitCasts()
        }
        return data
    }

    // TODO: cast `condition` expression to boolean?
    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: IrElement): IrElement {
        val loop = data as IrWhileLoop
        (loop.body as? IrContainerExpression)?.let {
            loop.body = it.insertImplicitCasts()
        }
        return data
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression, data: IrElement): IrElement = data

    override fun visitContinueExpression(continueExpression: FirContinueExpression, data: IrElement): IrElement = data

    // ==================================================================================

    override fun visitTryExpression(tryExpression: FirTryExpression, data: IrElement): IrElement {
        val irTry = data as IrTry
        (irTry.finallyExpression as? IrContainerExpression)?.let {
            irTry.finallyExpression = it.insertImplicitCasts()
        }
        return data
    }

    override fun visitThrowExpression(throwExpression: FirThrowExpression, data: IrElement): IrElement =
        (data as IrThrow).cast(throwExpression, throwExpression.exception.typeRef, throwExpression.typeRef)

    override fun visitBlock(block: FirBlock, data: IrElement): IrElement =
        (data as? IrContainerExpression)?.insertImplicitCasts() ?: data

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: IrElement): IrElement {
        val irReturn = data as IrReturn
        val expectedType = returnExpression.target.labeledElement.returnTypeRef
        irReturn.value = irReturn.value.cast(returnExpression.result, returnExpression.result.typeRef, expectedType)
        return data
    }

    // ==================================================================================

    internal fun IrExpression.cast(expression: FirExpression, valueType: FirTypeRef, expectedType: FirTypeRef): IrExpression {
        if (this is IrTypeOperatorCall) {
            return this
        }

        if (this is IrContainerExpression) {
            insertImplicitCasts()
        }

        return when {
            expectedType.isUnit -> {
                coerceToUnitIfNeeded(this, irBuiltIns)
            }
            valueType.coneTypeSafe<ConeDynamicType>() != null -> {
                if (expectedType.coneType !is ConeDynamicType && !expectedType.isNullableAny) {
                    implicitCast(this, expectedType.toIrType(ConversionTypeContext.DEFAULT))
                } else {
                    this
                }
            }
            typeCanBeEnhancedOrFlexibleNullable(valueType) && !expectedType.acceptsNullValues() -> {
                insertImplicitNotNullCastIfNeeded(expression)
            }
            // TODO: coerceIntToAnotherIntegerType
            // TODO: even implicitCast call can be here?
            else -> this
        }
    }

    private fun FirTypeRef.acceptsNullValues(): Boolean =
        canBeNull || hasEnhancedNullability()

    private fun IrExpression.insertImplicitNotNullCastIfNeeded(expression: FirExpression): IrExpression {
        if (this is IrGetEnumValue) return this
        // [TypeOperatorLowering] will retrieve the source (from start offset to end offset) as an assertion message.
        // Avoid type casting if we can't determine the source for some reasons, e.g., implicit `this` receiver.
        if (expression.source == null) return this
        return implicitNotNullCast(this)
    }

    private fun IrContainerExpression.insertImplicitCasts(): IrContainerExpression {
        if (statements.isEmpty()) return this

        val lastIndex = statements.lastIndex
        statements.forEachIndexed { i, irStatement ->
            if (irStatement !is IrErrorCallExpression && irStatement is IrExpression) {
                if (i != lastIndex) {
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
            implicitCastOrExpression(data as IrExpression, smartCastExpression.typeRef)
        } else {
            data as IrExpression
        }
    }

    internal fun implicitCastFromDispatchReceiver(
        original: IrExpression,
        originalTypeRef: FirTypeRef,
        calleeReference: FirReference?,
        conversionTypeContext: ConversionTypeContext,
    ): IrExpression {
        val referencedDeclaration = calleeReference?.toResolvedCallableSymbol()?.unwrapCallRepresentative()?.fir

        val dispatchReceiverType =
            referencedDeclaration?.dispatchReceiverType as? ConeClassLikeType
                ?: return implicitCastOrExpression(original, originalTypeRef)

        val starProjectedDispatchReceiver = dispatchReceiverType.replaceArgumentsWithStarProjections()

        val castType = originalTypeRef.coneTypeSafe<ConeIntersectionType>()
        castType?.intersectedTypes?.forEach { componentType ->
            if (AbstractTypeChecker.isSubtypeOf(session.typeContext, componentType, starProjectedDispatchReceiver)) {
                return implicitCastOrExpression(original, componentType, conversionTypeContext)
            }
        }

        return implicitCastOrExpression(original, originalTypeRef, conversionTypeContext)
    }

    private fun implicitCastOrExpression(
        original: IrExpression, castType: ConeKotlinType, conversionTypeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
    ): IrExpression {
        return implicitCastOrExpression(original, castType.toIrType(conversionTypeContext))
    }

    private fun implicitCastOrExpression(
        original: IrExpression, castType: FirTypeRef, conversionTypeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
    ): IrExpression {
        return implicitCastOrExpression(original, castType.toIrType(conversionTypeContext))
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

            return IrTypeOperatorCallImpl(
                original.startOffset,
                original.endOffset,
                castType,
                typeOperator,
                castType,
                original
            )
        }

        private fun coerceToUnitIfNeeded(original: IrExpression, irBuiltIns: IrBuiltIns): IrExpression {
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
            val castType = original.type.removeAnnotations {
                val classId = it.symbol.owner.parentAsClass.classId
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

        internal fun typeCanBeEnhancedOrFlexibleNullable(typeRef: FirTypeRef): Boolean {
            return when {
                typeRef.hasEnhancedNullability() -> true
                typeRef.isNullabilityFlexible() && typeRef.canBeNull -> true
                else -> false
            }
        }

        private fun FirTypeRef.isNullabilityFlexible(): Boolean {
            val flexibility = coneTypeSafe<ConeFlexibleType>() ?: return false
            return flexibility.lowerBound.isMarkedNullable != flexibility.upperBound.isMarkedNullable
        }
    }
}
