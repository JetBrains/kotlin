/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.types.model.CapturedTypeConstructorMarker
import org.jetbrains.kotlin.types.model.CapturedTypeMarker
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

abstract class IrTypeBase(val kotlinType: KotlinType?) : IrType, IrTypeProjection {
    override val type: IrType get() = this
}

class IrErrorTypeImpl(
    kotlinType: KotlinType?,
    override val annotations: List<IrConstructorCall>,
    override val variance: Variance,
) : IrTypeBase(kotlinType), IrErrorType {
    override fun equals(other: Any?): Boolean = other is IrErrorTypeImpl

    override fun hashCode(): Int = IrErrorTypeImpl::class.java.hashCode()
}

class IrDynamicTypeImpl(
    kotlinType: KotlinType?,
    override val annotations: List<IrConstructorCall>,
    override val variance: Variance,
) : IrTypeBase(kotlinType), IrDynamicType {
    override fun equals(other: Any?): Boolean = other is IrDynamicTypeImpl

    override fun hashCode(): Int = IrDynamicTypeImpl::class.java.hashCode()
}


val IrType.originalKotlinType: KotlinType?
    get() = safeAs<IrTypeBase>()?.kotlinType


object IrStarProjectionImpl : IrStarProjection {
    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * An instance which should be used when creating an IR element whose type cannot be determined at the moment of creation.
 *
 * Example: when translating generic functions in psi2ir, we're creating an IrFunction first, then adding IrTypeParameter instances to it,
 * and only then translating the function's return type with respect to those created type parameters.
 *
 * Instead of using this special instance, we could just make IrFunction/IrConstructor constructors allow to accept no return type,
 * however this could lead to a situation where we forget to set return type sometimes. This would result in crashes at unexpected moments,
 * especially in Kotlin/JS where function return types are not present in the resulting binary files.
 */
object IrUninitializedType : IrType {
    override val annotations: List<IrConstructorCall> = emptyList()

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}

class ReturnTypeIsNotInitializedException(function: IrFunction) : IllegalStateException(
    "Return type is not initialized for function '${function.name}'"
)


// Please note this type is not denotable which means it could only exist inside type system
class IrCapturedType(
    val captureStatus: CaptureStatus,
    val lowerType: IrType?,
    projection: IrTypeArgument,
    typeParameter: IrTypeParameter
) : IrSimpleType, CapturedTypeMarker {

    class Constructor(val argument: IrTypeArgument, val typeParameter: IrTypeParameter) :
        CapturedTypeConstructorMarker {

        private var _superTypes: List<IrType> = emptyList()

        val superTypes: List<IrType> get() = _superTypes

        fun initSuperTypes(superTypes: List<IrType>) {
            _superTypes = superTypes
        }
    }

    val constructor: Constructor = Constructor(projection, typeParameter)

    override val classifier: IrClassifierSymbol get() = error("Captured Type does not have a classifier")
    override val arguments: List<IrTypeArgument> get() = emptyList()
    override val abbreviation: IrTypeAbbreviation? get () = null
    override val hasQuestionMark: Boolean get() = false
    override val annotations: List<IrConstructorCall> get() = emptyList()

    override fun equals(other: Any?): Boolean {
        return other is IrCapturedType
                && captureStatus == other.captureStatus
                && lowerType == other.lowerType
                && constructor.argument == other.constructor.argument
                && constructor.typeParameter == other.constructor.typeParameter
                && constructor.superTypes == other.constructor.superTypes
    }

    override fun hashCode(): Int {
        return (((captureStatus.hashCode() * 31
                + (lowerType?.hashCode() ?: 0)) * 31
                + constructor.argument.hashCode()) * 31
                + constructor.typeParameter.hashCode()) + constructor.superTypes.hashCode()
    }
}