/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import com.intellij.psi.PsiCompiledElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.types.Variance

internal fun <T : IrElement> FirElement.convertWithOffsets(
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    if (psi is PsiCompiledElement) return f(-1, -1)
    val startOffset = psi?.startOffsetSkippingComments ?: -1
    val endOffset = psi?.endOffset ?: -1
    return f(startOffset, endOffset)
}

internal fun createErrorType(): IrErrorType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)

fun FirTypeRef.toIrType(session: FirSession, declarationStorage: Fir2IrDeclarationStorage, irBuiltIns: IrBuiltIns): IrType {
    if (this !is FirResolvedTypeRef) {
        return createErrorType()
    }
    return type.toIrType(session, declarationStorage, irBuiltIns)
}

fun ConeKotlinType.toIrType(
    session: FirSession,
    declarationStorage: Fir2IrDeclarationStorage,
    irBuiltIns: IrBuiltIns,
    definitelyNotNull: Boolean = false
): IrType {
    return when (this) {
        is ConeKotlinErrorType -> createErrorType()
        is ConeLookupTagBasedType -> {
            val irSymbol = getPrimitiveArrayType(this.classId, irBuiltIns) ?: run {
                val firSymbol = this.lookupTag.toSymbol(session) ?: return createErrorType()
                firSymbol.toIrSymbol(session, declarationStorage)
            }
            // TODO: annotations
            IrSimpleTypeImpl(
                irSymbol, !definitelyNotNull && this.isMarkedNullable,
                typeArguments.map { it.toIrTypeArgument(session, declarationStorage, irBuiltIns) },
                emptyList()
            )
        }
        is ConeFlexibleType -> {
            // TODO: yet we take more general type. Not quite sure it's Ok
            upperBound.toIrType(session, declarationStorage, irBuiltIns, definitelyNotNull)
        }
        is ConeCapturedType -> TODO()
        is ConeDefinitelyNotNullType -> {
            original.toIrType(session, declarationStorage, irBuiltIns, definitelyNotNull = true)
        }
        is ConeIntersectionType -> {
            // TODO: add intersectionTypeApproximation
            intersectedTypes.first().toIrType(session, declarationStorage, irBuiltIns, definitelyNotNull)
        }
        is ConeStubType -> createErrorType()
        is ConeIntegerLiteralType -> getApproximatedType().toIrType(session, declarationStorage, irBuiltIns, definitelyNotNull)
    }
}

private fun getPrimitiveArrayType(classId: ClassId?, irBuiltIns: IrBuiltIns): IrClassifierSymbol? {
    val irType = when (classId) {
        ClassId(FqName("kotlin"), FqName("BooleanArray"), false) -> irBuiltIns.booleanType
        ClassId(FqName("kotlin"), FqName("ByteArray"), false) -> irBuiltIns.byteType
        ClassId(FqName("kotlin"), FqName("CharArray"), false) -> irBuiltIns.charType
        ClassId(FqName("kotlin"), FqName("DoubleArray"), false) -> irBuiltIns.doubleType
        ClassId(FqName("kotlin"), FqName("FloatArray"), false) -> irBuiltIns.floatType
        ClassId(FqName("kotlin"), FqName("IntArray"), false) -> irBuiltIns.intType
        ClassId(FqName("kotlin"), FqName("LongArray"), false) -> irBuiltIns.longType
        ClassId(FqName("kotlin"), FqName("ShortArray"), false) -> irBuiltIns.shortType
        else -> null
    }
    return irType?.let { irBuiltIns.primitiveArrayForType.getValue(it) }
}

fun ConeKotlinTypeProjection.toIrTypeArgument(
    session: FirSession,
    declarationStorage: Fir2IrDeclarationStorage,
    irBuiltIns: IrBuiltIns
): IrTypeArgument {
    return when (this) {
        ConeStarProjection -> IrStarProjectionImpl
        is ConeKotlinTypeProjectionIn -> {
            val irType = this.type.toIrType(session, declarationStorage, irBuiltIns)
            makeTypeProjection(irType, Variance.IN_VARIANCE)
        }
        is ConeKotlinTypeProjectionOut -> {
            val irType = this.type.toIrType(session, declarationStorage, irBuiltIns)
            makeTypeProjection(irType, Variance.OUT_VARIANCE)
        }
        is ConeKotlinType -> {
            val irType = toIrType(session, declarationStorage, irBuiltIns)
            makeTypeProjection(irType, Variance.INVARIANT)
        }
    }
}

fun FirClassifierSymbol<*>.toIrSymbol(session: FirSession, declarationStorage: Fir2IrDeclarationStorage): IrClassifierSymbol {
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
    return when (this) {
        is FirResolvedNamedReference -> resolvedSymbol.toSymbol(declarationStorage)
        is FirThisReference -> {
            when (val boundSymbol = boundSymbol?.toSymbol(declarationStorage)) {
                is IrClassSymbol -> boundSymbol.owner.thisReceiver?.symbol
                is IrFunctionSymbol -> boundSymbol.owner.extensionReceiverParameter?.symbol
                else -> null
            }
        }
        else -> null
    }
}

private fun AbstractFirBasedSymbol<*>.toSymbol(declarationStorage: Fir2IrDeclarationStorage): IrSymbol? = when (this) {
    is FirClassSymbol -> toClassSymbol(declarationStorage)
    is FirFunctionSymbol<*> -> toFunctionSymbol(declarationStorage)
    is FirPropertySymbol -> if (fir.isLocal) toValueSymbol(declarationStorage) else toPropertyOrFieldSymbol(declarationStorage)
    is FirFieldSymbol -> toPropertyOrFieldSymbol(declarationStorage)
    is FirBackingFieldSymbol -> toBackingFieldSymbol(declarationStorage)
    is FirDelegateFieldSymbol<*> -> toBackingFieldSymbol(declarationStorage)
    is FirVariableSymbol<*> -> toValueSymbol(declarationStorage)
    else -> null
}

fun FirClassSymbol<*>.toClassSymbol(declarationStorage: Fir2IrDeclarationStorage): IrClassSymbol {
    return declarationStorage.getIrClassSymbol(this)
}

fun FirTypeParameterSymbol.toTypeParameterSymbol(declarationStorage: Fir2IrDeclarationStorage): IrTypeParameterSymbol {
    return declarationStorage.getIrTypeParameterSymbol(this)
}

fun FirFunctionSymbol<*>.toFunctionSymbol(declarationStorage: Fir2IrDeclarationStorage): IrFunctionSymbol {
    return declarationStorage.getIrFunctionSymbol(this)
}

fun FirVariableSymbol<*>.toPropertyOrFieldSymbol(declarationStorage: Fir2IrDeclarationStorage): IrSymbol {
    return declarationStorage.getIrPropertyOrFieldSymbol(this)
}

fun FirVariableSymbol<*>.toBackingFieldSymbol(declarationStorage: Fir2IrDeclarationStorage): IrSymbol {
    return declarationStorage.getIrBackingFieldSymbol(this)
}

fun FirVariableSymbol<*>.toValueSymbol(declarationStorage: Fir2IrDeclarationStorage): IrSymbol {
    return declarationStorage.getIrValueSymbol(this)
}
