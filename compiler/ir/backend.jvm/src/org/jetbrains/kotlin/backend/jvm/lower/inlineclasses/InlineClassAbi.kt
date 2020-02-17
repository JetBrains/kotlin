/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.inlineclasses

import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.codegen.state.md5base64
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils

/**
 * Replace inline classes by their underlying types.
 */
fun IrType.unboxInlineClass() = InlineClassAbi.unboxType(this) ?: this

object InlineClassAbi {
    /**
     * Unwraps inline class types to their underlying representation.
     * Returns null if the type cannot be unboxed.
     */
    internal fun unboxType(type: IrType): IrType? {
        val klass = type.classOrNull?.owner ?: return null
        if (!klass.isInline) return null

        // TODO: Apply type substitutions
        val underlyingType = getUnderlyingType(klass).unboxInlineClass()
        if (!type.isNullable())
            return underlyingType
        if (underlyingType.isNullable() || underlyingType.isPrimitiveType())
            return null
        return underlyingType.makeNullable()
    }

    /**
     * Get the underlying type of an inline class based on the single argument to its
     * primary constructor. This is what the current jvm backend does.
     *
     * Looking for a backing field does not work for unsigned types, which don't
     * contain a field.
     */
    fun getUnderlyingType(irClass: IrClass): IrType {
        require(irClass.isInline)
        return irClass.primaryConstructor!!.valueParameters[0].type
    }

    /**
     * Returns a mangled name for a function taking inline class arguments
     * to avoid clashes between overloaded methods.
     */
    fun mangledNameFor(irFunction: IrFunction): Name {
        val suffix = when {
            irFunction.fullValueParameterList.any { it.type.requiresMangling } ->
                hashSuffix(irFunction)
            (irFunction.parent as? IrClass)?.isInline == true -> "impl"
            else -> return irFunction.name
        }

        val base = when {
            irFunction is IrConstructor ->
                "constructor"
            irFunction.isGetter ->
                JvmAbi.getterName(irFunction.propertyName.asString())
            irFunction.isSetter ->
                JvmAbi.setterName(irFunction.propertyName.asString())
            irFunction.name.isSpecial ->
                error("Unhandled special name in mangledNameFor: ${irFunction.name}")
            else ->
                irFunction.name.asString()
        }

        return Name.identifier("$base-$suffix")
    }

    private val IrFunction.propertyName: Name
        get() = (this as IrSimpleFunction).correspondingPropertySymbol!!.owner.name

    private fun hashSuffix(irFunction: IrFunction) =
        md5base64(irFunction.fullValueParameterList.joinToString { it.type.eraseToString() })

    private fun IrType.eraseToString() = buildString {
        append('L')
        append(erasedUpperBound.fqNameWhenAvailable!!)
        if (isNullable()) append('?')
        append(';')
    }
}

private val IrType.requiresMangling: Boolean
    get() {
        val irClass = erasedUpperBound
        return irClass.isInline && irClass.fqNameWhenAvailable != DescriptorUtils.RESULT_FQ_NAME
    }

private val IrFunction.fullValueParameterList: List<IrValueParameter>
    get() = listOfNotNull(extensionReceiverParameter) + valueParameters

internal val IrFunction.hasMangledParameters: Boolean
    get() = dispatchReceiverParameter != null && parentAsClass.isInline ||
            fullValueParameterList.any { it.type.requiresMangling } ||
            (this is IrConstructor && constructedClass.isInline)

internal val IrClass.inlineClassFieldName: Name
    get() = primaryConstructor!!.valueParameters.single().name

val IrFunction.isInlineClassFieldGetter: Boolean
    get() = (parent as? IrClass)?.isInline == true && this is IrSimpleFunction && extensionReceiverParameter == null &&
            correspondingPropertySymbol?.let { it.owner.getter == this && it.owner.name == parentAsClass.inlineClassFieldName } == true

val IrFunction.isPrimaryInlineClassConstructor: Boolean
    get() = this is IrConstructor && isPrimary && constructedClass.isInline
