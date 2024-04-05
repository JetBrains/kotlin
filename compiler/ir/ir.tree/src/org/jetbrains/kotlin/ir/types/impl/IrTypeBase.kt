/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.types.model.CapturedTypeConstructorMarker
import org.jetbrains.kotlin.types.model.CapturedTypeMarker

abstract class IrTypeBase(val kotlinType: KotlinType?) : IrType(), IrTypeProjection {
    override val type: IrType get() = this
}

class IrErrorTypeImpl(
    kotlinType: KotlinType?,
    override val annotations: List<IrConstructorCall>,
    override val variance: Variance,
    isMarkedNullable: Boolean = false
) : IrErrorType(kotlinType, IrErrorClassImpl.symbol, isMarkedNullable) {
    override fun equals(other: Any?): Boolean = other is IrErrorTypeImpl

    override fun hashCode(): Int = IrErrorTypeImpl::class.java.hashCode()
}

class IrDynamicTypeImpl(
    kotlinType: KotlinType?,
    override val annotations: List<IrConstructorCall>,
    override val variance: Variance,
) : IrDynamicType(kotlinType) {
    override fun equals(other: Any?): Boolean = other is IrDynamicTypeImpl

    override fun hashCode(): Int = IrDynamicTypeImpl::class.java.hashCode()
}

val IrType.originalKotlinType: KotlinType?
    get() = (this as? IrTypeBase)?.kotlinType

data object IrStarProjectionImpl : IrStarProjection

// Please note this type is not denotable which means it could only exist inside type system
class IrCapturedType(
    val captureStatus: CaptureStatus,
    val lowerType: IrType?,
    projection: IrTypeArgument,
    typeParameter: IrTypeParameter,
    override val nullability: SimpleTypeNullability,
    override val annotations: List<IrConstructorCall>,
    override val abbreviation: IrTypeAbbreviation?,
) : IrSimpleType(null), CapturedTypeMarker {
    class Constructor(val argument: IrTypeArgument, val typeParameter: IrTypeParameter) : CapturedTypeConstructorMarker {
        var superTypes: List<IrType> = emptyList()
            private set

        fun initSuperTypes(superTypes: List<IrType>) {
            this.superTypes = superTypes
        }
    }

    val constructor: Constructor = Constructor(projection, typeParameter)

    override val classifier: IrClassifierSymbol get() = error("Captured Type does not have a classifier")

    override val arguments: List<IrTypeArgument> get() = emptyList()

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)

    override fun toString(): String = render()
}
