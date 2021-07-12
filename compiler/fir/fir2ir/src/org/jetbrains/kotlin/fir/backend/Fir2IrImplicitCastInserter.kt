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
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.types.AbstractTypeChecker

class Fir2IrImplicitCastInserter(
    private val components: Fir2IrComponents
) : Fir2IrComponents by components, FirDefaultVisitor<IrElement, IrElement>() {

    private fun FirTypeRef.toIrType(): IrType = with(typeConverter) { toIrType() }

    private fun ConeKotlinType.toIrType(): IrType = with(typeConverter) { toIrType() }

    override fun visitElement(element: FirElement, data: IrElement): IrElement {
        TODO("Should not be here: ${element::class}: ${element.render()}")
    }

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
            is FirUnitExpression -> (data as IrExpression).let { it.coerceToUnitIfNeeded(it.type, irBuiltIns) }
            else -> data
        }
    }

    override fun visitStatement(statement: FirStatement, data: IrElement): IrElement {
        return when (statement) {
            is FirTypeAlias -> data
            FirStubStatement -> data
            is FirUnitExpression -> (data as IrExpression).let { it.coerceToUnitIfNeeded(it.type, irBuiltIns) }
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
        return when {
            this is IrContainerExpression -> {
                insertImplicitCasts()
            }
            expectedType.isUnit -> {
                coerceToUnitIfNeeded(type, irBuiltIns)
            }
            valueType.isNullabilityFlexible() && valueType.canBeNull && !expectedType.acceptsNullValues() -> {
                insertImplicitNotNullCastIfNeeded(expression)
            }
            valueType.hasEnhancedNullability() && !expectedType.acceptsNullValues() -> {
                insertImplicitNotNullCastIfNeeded(expression)
            }
            // TODO: coerceIntToAnotherIntegerType
            // TODO: even implicitCast call can be here?
            else -> this
        }
    }

    private fun FirTypeRef.isNullabilityFlexible(): Boolean {
        val flexibility = coneTypeSafe<ConeFlexibleType>() ?: return false
        return flexibility.lowerBound.isMarkedNullable != flexibility.upperBound.isMarkedNullable
    }

    private fun FirTypeRef.acceptsNullValues(): Boolean =
        canBeNull || hasEnhancedNullability()

    private fun IrExpression.insertImplicitNotNullCastIfNeeded(expression: FirExpression): IrExpression {
        if (this is IrGetEnumValue) return this
        // [TypeOperatorLowering] will retrieve the source (from start offset to end offset) as an assertion message.
        // Avoid type casting if we can't determine the source for some reasons, e.g., implicit `this` receiver.
        if (expression.source == null) return this
        // Cast type massage 1. Remove @EnhancedNullability
        // Cast type massage 2. Convert it to a non-null variant (in case of @FlexibleNullability)
        val castType = type.removeAnnotations {
            it.symbol.owner.parentAsClass.classId == CompilerConeAttributes.EnhancedNullability.ANNOTATION_CLASS_ID
        }.withHasQuestionMark(false)
        return IrTypeOperatorCallImpl(
            this.startOffset,
            this.endOffset,
            castType,
            IrTypeOperator.IMPLICIT_NOTNULL,
            castType,
            this
        )
    }

    private fun IrContainerExpression.insertImplicitCasts(): IrContainerExpression {
        if (statements.isEmpty()) return this

        val lastIndex = statements.lastIndex
        statements.forEachIndexed { i, irStatement ->
            if (irStatement !is IrErrorCallExpression && irStatement is IrExpression) {
                if (i != lastIndex) {
                    statements[i] = irStatement.coerceToUnitIfNeeded(irStatement.type, irBuiltIns)
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
                statements[i] = irStatement.coerceToUnitIfNeeded(irStatement.type, irBuiltIns)
            }
        }
        return this
    }

    override fun visitExpressionWithSmartcast(expressionWithSmartcast: FirExpressionWithSmartcast, data: IrElement): IrExpression {
        return if (expressionWithSmartcast.isStable) {
            implicitCastOrExpression(data as IrExpression, expressionWithSmartcast.typeRef)
        } else {
            data as IrExpression
        }
    }

    override fun visitExpressionWithSmartcastToNull(
        expressionWithSmartcastToNull: FirExpressionWithSmartcastToNull,
        data: IrElement
    ): IrElement {
        // We don't want an implicit cast to Nothing?. This expression just encompasses nullability after null check.
        return data
    }

    internal fun implicitCastFromDispatchReceiver(
        original: IrExpression,
        originalTypeRef: FirTypeRef,
        calleeReference: FirReference,
    ): IrExpression {
        val referencedDeclaration =
            ((calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirCallableSymbol<*>)?.unwrapCallRepresentative()
                ?.fir

        val dispatchReceiverType =
            referencedDeclaration?.dispatchReceiverType as? ConeClassLikeType
                ?: return implicitCastOrExpression(original, originalTypeRef)

        val starProjectedDispatchReceiver = dispatchReceiverType.replaceArgumentsWithStarProjections()

        val castType = originalTypeRef.coneTypeSafe<ConeIntersectionType>()
        castType?.intersectedTypes?.forEach { componentType ->
            if (AbstractTypeChecker.isSubtypeOf(session.typeContext, componentType, starProjectedDispatchReceiver)) {
                return implicitCastOrExpression(original, componentType)
            }
        }

        return implicitCastOrExpression(original, originalTypeRef)
    }

    private fun implicitCastOrExpression(original: IrExpression, castType: ConeKotlinType): IrExpression {
        return implicitCastOrExpression(original, castType.toIrType())
    }

    private fun implicitCastOrExpression(original: IrExpression, castType: FirTypeRef): IrExpression {
        return implicitCastOrExpression(original, castType.toIrType())
    }

    internal fun implicitCastOrExpression(original: IrExpression, castType: IrType): IrExpression {
        if (original.type.makeNotNull() == castType.makeNotNull()) return original
        return implicitCast(original, castType)
    }

    private fun implicitCast(original: IrExpression, castType: IrType): IrExpression {
        return IrTypeOperatorCallImpl(
            original.startOffset,
            original.endOffset,
            castType,
            IrTypeOperator.IMPLICIT_CAST,
            castType,
            original
        )
    }

    private fun IrExpression.coerceToUnitIfNeeded(valueType: IrType, irBuiltIns: IrBuiltIns): IrExpression {
        return if (valueType.isUnit() || valueType.isNothing())
            this
        else
            IrTypeOperatorCallImpl(
                startOffset, endOffset,
                irBuiltIns.unitType,
                IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
                irBuiltIns.unitType,
                this
            )
    }
}
