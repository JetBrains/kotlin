/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

class Fir2IrTypeConverter(
    private val components: Fir2IrComponents
) : Fir2IrComponents by components {
    lateinit var nothingType: IrType
    lateinit var unitType: IrType
    lateinit var booleanType: IrType
    lateinit var stringType: IrType

    fun initBuiltinTypes() {
        nothingType = session.builtinTypes.nothingType.toIrType()
        unitType = session.builtinTypes.unitType.toIrType()
        booleanType = session.builtinTypes.booleanType.toIrType()
        stringType = session.builtinTypes.stringType.toIrType()
    }

    fun FirTypeRef.toIrType(typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT): IrType {
        if (this !is FirResolvedTypeRef) {
            return createErrorType()
        }
        return type.toIrType(typeContext)
    }

    fun ConeKotlinType.toIrType(typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT): IrType {
        return when (this) {
            is ConeKotlinErrorType -> createErrorType()
            is ConeLookupTagBasedType -> {
                val irSymbol = getArrayType(this.classId) ?: run {
                    val firSymbol = this.lookupTag.toSymbol(session) ?: return createErrorType()
                    firSymbol.toSymbol(session, classifierStorage, typeContext)
                }
                // TODO: annotations
                IrSimpleTypeImpl(
                    irSymbol, !typeContext.definitelyNotNull && this.isMarkedNullable,
                    typeArguments.map { it.toIrTypeArgument() },
                    emptyList()
                )
            }
            is ConeFlexibleType -> {
                // TODO: yet we take more general type. Not quite sure it's Ok
                upperBound.toIrType(typeContext)
            }
            is ConeCapturedType -> {
                lowerType?.toIrType(typeContext) ?: constructor.supertypes!!.first().toIrType(typeContext)
            }
            is ConeDefinitelyNotNullType -> {
                original.toIrType(typeContext.definitelyNotNull())
            }
            is ConeIntersectionType -> {
                // TODO: add intersectionTypeApproximation
                intersectedTypes.first().toIrType(typeContext)
            }
            is ConeStubType -> createErrorType()
            is ConeIntegerLiteralType -> getApproximatedType().toIrType(typeContext)
        }
    }

    private fun ConeTypeProjection.toIrTypeArgument(): IrTypeArgument {
        return when (this) {
            ConeStarProjection -> IrStarProjectionImpl
            is ConeKotlinTypeProjectionIn -> {
                val irType = this.type.toIrType()
                makeTypeProjection(irType, Variance.IN_VARIANCE)
            }
            is ConeKotlinTypeProjectionOut -> {
                val irType = this.type.toIrType()
                makeTypeProjection(irType, Variance.OUT_VARIANCE)
            }
            is ConeKotlinType -> {
                val irType = toIrType()
                makeTypeProjection(irType, Variance.INVARIANT)
            }
        }
    }

    private fun getArrayType(classId: ClassId?): IrClassifierSymbol? {
        if (classId == StandardClassIds.Array) {
            return irBuiltIns.arrayClass
        }
        val primitiveId = StandardClassIds.elementTypeByPrimitiveArrayType[classId] ?: return null
        val irType = when (primitiveId) {
            StandardClassIds.Boolean -> irBuiltIns.booleanType
            StandardClassIds.Byte -> irBuiltIns.byteType
            StandardClassIds.Char -> irBuiltIns.charType
            StandardClassIds.Double -> irBuiltIns.doubleType
            StandardClassIds.Float -> irBuiltIns.floatType
            StandardClassIds.Int -> irBuiltIns.intType
            StandardClassIds.Long -> irBuiltIns.longType
            StandardClassIds.Short -> irBuiltIns.shortType
            else -> throw AssertionError("Strange primitiveId $primitiveId from array: $classId")
        }
        return irBuiltIns.primitiveArrayForType.getValue(irType)
    }
}