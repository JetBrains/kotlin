/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.unboxInlineClass
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.unexpectedSymbolKind

/**
 * Perform as much type erasure as is significant for JVM signature generation.
 * Class types are kept as is, while type parameters are replaced with their
 * erased upper bounds, keeping the nullability information.
 *
 * For example, a type parameter `T?` where `T : Any`, `T : Comparable<T>` is
 * erased to `Any?`.
 *
 * Type arguments to the erased upper bound are replaced by `*`, since
 * recursive erasure could loop. For example, a type parameter
 * `T : Comparable<T>` is replaced by `Comparable<*>`.
 */
fun IrType.eraseTypeParameters(): IrType = when (this) {
    is IrSimpleType ->
        when (val owner = classifier.owner) {
            is IrScript -> {
                assert(arguments.isEmpty()) { "Script can't be generic: " + owner.render() }
                IrSimpleTypeImpl(classifier, nullability, emptyList(), annotations)
            }
            is IrClass -> IrSimpleTypeImpl(classifier, nullability, arguments.map { it.eraseTypeParameters() }, annotations)
            is IrTypeParameter -> owner.erasedType(isNullable())
            else -> error("Unknown IrSimpleType classifier kind: $owner")
        }
    is IrErrorType ->
        this
    else -> error("Unknown IrType kind: $this")
}

fun IrType.eraseIfTypeParameter(): IrType {
    val typeParameter = (this as? IrSimpleType)?.classifier?.owner as? IrTypeParameter ?: return this
    return typeParameter.erasedType(isNullable())
}

private fun IrTypeParameter.erasedType(isNullable: Boolean): IrType {
    val upperBound = erasedUpperBound
    return IrSimpleTypeImpl(
        upperBound.symbol,
        isNullable,
        // Should not affect JVM signature, but may result in an invalid type object
        List(upperBound.typeParameters.size) { IrStarProjectionImpl },
        annotations
    )
}

private fun IrTypeArgument.eraseTypeParameters(): IrTypeArgument = when (this) {
    is IrStarProjection -> this
    is IrTypeProjection -> makeTypeProjection(type.eraseTypeParameters(), variance)
}

/**
 * Computes the erased class for this type parameter according to the java erasure rules.
 */
val IrTypeParameter.erasedUpperBound: IrClass
    get() {
        // Pick the (necessarily unique) non-interface upper bound if it exists
        for (type in superTypes) {
            val irClass = type.classOrNull?.owner ?: continue
            if (!irClass.isJvmInterface) return irClass
        }

        // Otherwise, choose either the first IrClass supertype or recurse.
        // In the first case, all supertypes are interface types and the choice was arbitrary.
        // In the second case, there is only a single supertype.
        return superTypes.first().erasedUpperBound
    }

val IrType.erasedUpperBound: IrClass
    get() = when (this) {
        is IrSimpleType -> when (val classifier = classifier.owner) {
            is IrClass -> classifier
            is IrTypeParameter -> classifier.erasedUpperBound
            is IrScript -> classifier.targetClass!!.owner
            else -> error(render())
        }
        is IrErrorType -> symbol.owner
        else -> error(render())
    }

/**
 * Get the default null/0 value for the type.
 *
 * This handles unboxing of non-nullable inline class types to their underlying types and produces
 * a null/0 default value for the resulting type. When such unboxing takes place it ensures that
 * the value is not reboxed and reunboxed by the codegen by using the unsafeCoerceIntrinsic.
 */
fun IrType.defaultValue(startOffset: Int, endOffset: Int, context: JvmBackendContext): IrExpression {
    val classifier = this.classifierOrNull
    if (classifier is IrTypeParameterSymbol) {
        return classifier.owner.representativeUpperBound.defaultValue(startOffset, endOffset, context)
    }

    if (this !is IrSimpleType || this.isMarkedNullable() || classOrNull?.owner?.isSingleFieldValueClass != true)
        return IrConstImpl.defaultValueForType(startOffset, endOffset, this)

    val underlyingType = unboxInlineClass()
    val defaultValueForUnderlyingType = IrConstImpl.defaultValueForType(startOffset, endOffset, underlyingType)
    return IrCallImpl.fromSymbolOwner(startOffset, endOffset, this, context.ir.symbols.unsafeCoerceIntrinsic).also {
        it.putTypeArgument(0, underlyingType) // from
        it.putTypeArgument(1, this) // to
        it.putValueArgument(0, defaultValueForUnderlyingType)
    }
}

fun IrType.isInlineClassType(): Boolean = erasedUpperBound.isSingleFieldValueClass

fun IrType.isMultiFieldValueClassType(): Boolean = erasedUpperBound.isMultiFieldValueClass

fun IrType.isValueClassType(): Boolean = erasedUpperBound.isValue

val IrType.upperBound: IrType
    get() = erasedUpperBound.symbol.starProjectedType

fun IrType.eraseToScope(scopeOwner: IrTypeParametersContainer): IrType =
    eraseToScope(collectVisibleTypeParameters(scopeOwner))

fun IrType.eraseToScope(visibleTypeParameters: Set<IrTypeParameter>): IrType {
    require(this is IrSimpleType) { error("Unexpected IrType kind: ${render()}") }
    return when (classifier) {
        is IrClassSymbol ->
            IrSimpleTypeImpl(
                classifier, nullability, arguments.map { it.eraseToScope(visibleTypeParameters) }, annotations
            )
        is IrTypeParameterSymbol ->
            if (classifier.owner in visibleTypeParameters)
                this
            else
                upperBound.mergeNullability(this)
        is IrScriptSymbol -> classifier.unexpectedSymbolKind<IrClassifierSymbol>()
    }
}

private fun IrTypeArgument.eraseToScope(visibleTypeParameters: Set<IrTypeParameter>): IrTypeArgument = when (this) {
    is IrStarProjection -> this
    is IrTypeProjection -> makeTypeProjection(type.eraseToScope(visibleTypeParameters), variance)
}

fun collectVisibleTypeParameters(scopeOwner: IrTypeParametersContainer): Set<IrTypeParameter> =
    generateSequence(scopeOwner) { current ->
        val parent = current.parent as? IrTypeParametersContainer
        parent.takeUnless { parent is IrClass && current is IrClass && !current.isInner && !current.isLocal }
    }
        .flatMap { it.typeParameters }
        .toSet()

val IrType.isReifiedTypeParameter: Boolean
    get() = (classifierOrNull as? IrTypeParameterSymbol)?.owner?.isReified == true

val IrTypeParameter.representativeUpperBound: IrType
    get() {
        assert(superTypes.isNotEmpty()) { "Upper bounds should not be empty: ${render()}" }

        return superTypes.firstOrNull {
            val irClass = it.classOrNull?.owner ?: return@firstOrNull false
            irClass.kind != ClassKind.INTERFACE && irClass.kind != ClassKind.ANNOTATION_CLASS
        } ?: superTypes.first()
    }
