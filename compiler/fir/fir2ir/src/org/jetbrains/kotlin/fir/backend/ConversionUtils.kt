/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.types.Variance

internal fun <T : IrElement> FirElement.convertWithOffsets(
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    val startOffset = psi?.startOffsetSkippingComments ?: -1
    val endOffset = psi?.endOffset ?: -1
    return f(startOffset, endOffset)
}

internal fun createErrorType(): IrErrorType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)

fun FirTypeRef.toIrType(session: FirSession, declarationStorage: Fir2IrDeclarationStorage): IrType {
    if (this !is FirResolvedTypeRef) {
        return createErrorType()
    }
    return type.toIrType(session, declarationStorage)
}

fun ConeKotlinType.toIrType(session: FirSession, declarationStorage: Fir2IrDeclarationStorage): IrType {
    return when (this) {
        is ConeKotlinErrorType -> createErrorType()
        is ConeLookupTagBasedType -> {
            val firSymbol = this.lookupTag.toSymbol(session) ?: return createErrorType()
            val irSymbol = firSymbol.toIrSymbol(session, declarationStorage)
            // TODO: annotations
            IrSimpleTypeImpl(
                irSymbol, this.isMarkedNullable,
                typeArguments.map { it.toIrTypeArgument(session, declarationStorage) },
                emptyList()
            )
        }
        is ConeFlexibleType -> {
            // TODO: yet we take more general type. Not quite sure it's Ok
            upperBound.toIrType(session, declarationStorage)
        }
        is ConeCapturedType -> TODO()
        is ConeDefinitelyNotNullType -> TODO()
    }
}

fun ConeKotlinTypeProjection.toIrTypeArgument(session: FirSession, declarationStorage: Fir2IrDeclarationStorage): IrTypeArgument {
    return when (this) {
        ConeStarProjection -> IrStarProjectionImpl
        is ConeKotlinTypeProjectionIn -> {
            val irType = this.type.toIrType(session, declarationStorage)
            makeTypeProjection(irType, Variance.IN_VARIANCE)
        }
        is ConeKotlinTypeProjectionOut -> {
            val irType = this.type.toIrType(session, declarationStorage)
            makeTypeProjection(irType, Variance.OUT_VARIANCE)
        }
        is ConeKotlinType -> {
            val irType = toIrType(session, declarationStorage)
            makeTypeProjection(irType, Variance.INVARIANT)
        }
    }
}

fun ConeClassifierSymbol.toIrSymbol(session: FirSession, declarationStorage: Fir2IrDeclarationStorage): IrClassifierSymbol {
    return when (this) {
        is FirTypeParameterSymbol -> {
            toTypeParameterSymbol(declarationStorage)
        }
        is FirTypeAliasSymbol -> {
            val typeAlias = fir
            val coneClassLikeType = (typeAlias.expandedTypeRef as FirResolvedTypeRef).type as ConeClassLikeType
            coneClassLikeType.lookupTag.toSymbol(session)!!.toIrSymbol(session, declarationStorage)
        }
        is FirClassSymbol -> {
            toClassSymbol(declarationStorage)
        }
        else -> throw AssertionError("Should not be here: $this")
    }
}

fun FirReference.toSymbol(declarationStorage: Fir2IrDeclarationStorage): IrSymbol? {
    if (this is FirNamedReference) {
        return toSymbol(declarationStorage)
    }
    return null
}

fun FirNamedReference.toSymbol(declarationStorage: Fir2IrDeclarationStorage): IrSymbol? {
    if (this is FirResolvedCallableReference) {
        when (val callableSymbol = this.coneSymbol) {
            is FirFunctionSymbol -> return callableSymbol.toFunctionSymbol(declarationStorage)
            is FirPropertySymbol -> return callableSymbol.toPropertyOrFieldSymbol(declarationStorage)
            is FirVariableSymbol -> return callableSymbol.toValueSymbol(declarationStorage)
        }
    }
    return null
}

fun FirClassSymbol.toClassSymbol(declarationStorage: Fir2IrDeclarationStorage): IrClassSymbol {
    return declarationStorage.getIrClassSymbol(this)
}

fun FirTypeParameterSymbol.toTypeParameterSymbol(declarationStorage: Fir2IrDeclarationStorage): IrTypeParameterSymbol {
    return declarationStorage.getIrTypeParameterSymbol(this)
}

fun FirFunctionSymbol.toFunctionSymbol(declarationStorage: Fir2IrDeclarationStorage): IrFunctionSymbol {
    return declarationStorage.getIrFunctionSymbol(this)
}

fun FirPropertySymbol.toPropertyOrFieldSymbol(declarationStorage: Fir2IrDeclarationStorage): IrSymbol {
    return declarationStorage.getIrPropertyOrFieldSymbol(this)
}

fun FirVariableSymbol.toValueSymbol(declarationStorage: Fir2IrDeclarationStorage): IrValueSymbol {
    return declarationStorage.getIrValueSymbol(this)
}
