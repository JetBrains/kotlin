/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.coerceToUnitIfNeeded
import org.jetbrains.kotlin.name.Name

class Fir2IrImplicitCastInserter(
    private val components: Fir2IrComponents,
    private val visitor: Fir2IrVisitor
) : Fir2IrComponents by components, FirDefaultVisitor<IrElement, IrElement>() {

    private fun FirTypeRef.toIrType(): IrType = with(typeConverter) { toIrType() }

    private fun ConeKotlinType.toIrType(): IrType = with(typeConverter) { toIrType() }

    override fun visitElement(element: FirElement, data: IrElement): IrElement {
        TODO("Should not be here: ${element.render()}")
    }

    // TODO: can be private once this visitor becomes more comprehensive
    internal fun IrContainerExpression.insertImplicitCasts(): IrContainerExpression {
        if (statements.isEmpty()) return this

        val lastIndex = statements.lastIndex
        statements.forEachIndexed { i, irStatement ->
            if (irStatement !is IrErrorCallExpression && irStatement is IrExpression) {
                if (i != lastIndex) {
                    statements[i] = irStatement.coerceToUnitIfNeeded(irStatement.type, irBuiltIns)
                } else {
                    // TODO: for the last statement, need to cast to the return type if mismatched
                }
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
        return implicitCastOrExpression(data as IrExpression, expressionWithSmartcast.typeRef)
    }

    internal fun convertToImplicitCastExpression(
        expressionWithSmartcast: FirExpressionWithSmartcast, calleeReference: FirReference
    ): IrExpression {
        val originalExpression = expressionWithSmartcast.originalExpression
        val value = visitor.convertToIrExpression(originalExpression)
        val castTypeRef = expressionWithSmartcast.typeRef
        if (calleeReference !is FirResolvedNamedReference) {
            return implicitCastOrExpression(value, castTypeRef)
        }
        val referencedSymbol = calleeReference.resolvedSymbol
        if (referencedSymbol !is FirPropertySymbol && referencedSymbol !is FirFunctionSymbol) {
            return implicitCastOrExpression(value, castTypeRef)
        }

        val originalTypeRef = expressionWithSmartcast.originalType
        if (castTypeRef is FirResolvedTypeRef && originalTypeRef is FirResolvedTypeRef) {
            val castType = castTypeRef.type
            if (castType is ConeIntersectionType) {
                val unwrappedSymbol = (referencedSymbol as? FirCallableSymbol)?.overriddenSymbol ?: referencedSymbol
                castType.intersectedTypes.forEach {
                    if (it.doesContainReferencedSymbolInScope(unwrappedSymbol, calleeReference.name)) {
                        return implicitCastOrExpression(value, it)
                    }
                }
            }
        }
        return if (originalExpression is FirThisReceiverExpression &&
            originalExpression.calleeReference.boundSymbol is FirAnonymousFunctionSymbol
        ) {
            // If the original is a "this" in a local function and original.type is the same as castType,
            // we still want to keep the cast. See kt-42517
            implicitCast(value, castTypeRef.toIrType())
        } else {
            implicitCastOrExpression(value, castTypeRef.toIrType())
        }
    }

    private fun ConeKotlinType.doesContainReferencedSymbolInScope(
        referencedSymbol: AbstractFirBasedSymbol<*>, name: Name
    ): Boolean {
        val scope = scope(session, components.scopeSession, FakeOverrideTypeCalculator.Forced) ?: return false
        var result = false
        val processor = { it: FirCallableSymbol<*> ->
            if (!result && it == referencedSymbol) {
                result = true
            }
        }
        when (referencedSymbol) {
            is FirPropertySymbol -> scope.processPropertiesByName(name, processor)
            is FirFunctionSymbol -> scope.processFunctionsByName(name, processor)
        }
        return result
    }

    private fun implicitCastOrExpression(original: IrExpression, castType: ConeKotlinType): IrExpression {
        return implicitCastOrExpression(original, castType.toIrType())
    }

    private fun implicitCastOrExpression(original: IrExpression, castType: FirTypeRef): IrExpression {
        return implicitCastOrExpression(original, castType.toIrType())
    }

    internal fun implicitCastOrExpression(original: IrExpression, castType: IrType): IrExpression {
        return original.takeIf { it.type == castType } ?: implicitCast(original, castType)
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
}
