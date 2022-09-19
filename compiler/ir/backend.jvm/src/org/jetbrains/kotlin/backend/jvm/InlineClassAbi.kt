/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.state.InfoForMangling
import org.jetbrains.kotlin.codegen.state.collectFunctionSignatureForManglingSuffix
import org.jetbrains.kotlin.codegen.state.md5base64
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name

/**
 * Replace inline classes by their underlying types.
 */
fun IrType.unboxInlineClass() = InlineClassAbi.unboxType(this) ?: this

object InlineClassAbi {
    /**
     * An origin for IrFunctionReferences which prevents inline class mangling. This only exists because of
     * inconsistencies between `RuntimeTypeMapper` and `KotlinTypeMapper`. The `RuntimeTypeMapper` does not
     * perform inline class mangling and so in the absence of jvm signatures in the metadata we need to avoid
     * inline class mangling as well in the function references used as arguments to the signature string intrinsic.
     */
    object UNMANGLED_FUNCTION_REFERENCE : IrStatementOriginImpl("UNMANGLED_FUNCTION_REFERENCE")

    /**
     * Unwraps inline class types to their underlying representation.
     * Returns null if the type cannot be unboxed.
     */
    fun unboxType(type: IrType): IrType? {
        val klass = type.classOrNull?.owner ?: return null
        val representation = klass.inlineClassRepresentation ?: return null

        // TODO: Apply type substitutions
        var underlyingType = representation.underlyingType.unboxInlineClass()
        if (!underlyingType.isNullable() && underlyingType.isTypeParameter()) {
            underlyingType = underlyingType.erasedUpperBound.defaultType
        }
        if (!type.isNullable())
            return underlyingType
        if (underlyingType.isNullable() || underlyingType.isPrimitiveType())
            return null
        return underlyingType.makeNullable()
    }

    /**
     * Returns a mangled name for a function taking inline class arguments
     * to avoid clashes between overloaded methods.
     */
    fun mangledNameFor(irFunction: IrFunction, mangleReturnTypes: Boolean, useOldMangleRules: Boolean): Name {
        if (irFunction is IrConstructor) {
            // Note that we might drop this convention and use standard mangling for constructors too, see KT-37186.
            assert(irFunction.constructedClass.isValue) {
                "Should not mangle names of non-inline class constructors: ${irFunction.render()}"
            }
            return Name.identifier("constructor${JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS}")
        }

        val suffix = hashSuffix(irFunction, mangleReturnTypes, useOldMangleRules)
        if (suffix == null && ((irFunction.parent as? IrClass)?.isValue != true || irFunction.origin == IrDeclarationOrigin.IR_BUILTINS_STUB)) {
            return irFunction.name
        }

        val base = when {
            irFunction.isGetter ->
                JvmAbi.getterName(irFunction.propertyName.asString())
            irFunction.isSetter ->
                JvmAbi.setterName(irFunction.propertyName.asString())
            irFunction.name.isSpecial ->
                error("Unhandled special name in mangledNameFor: ${irFunction.name}")
            else ->
                irFunction.name.asString()
        }

        return Name.identifier("$base${if (suffix == null) JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS else "-$suffix"}")
    }

    fun hashSuffix(irFunction: IrFunction, mangleReturnTypes: Boolean, useOldMangleRules: Boolean): String? =
        hashSuffix(
            useOldMangleRules,
            irFunction.fullValueParameterList.map { it.type },
            irFunction.returnType.takeIf { mangleReturnTypes && irFunction.hasMangledReturnType },
            irFunction.isSuspend
        )

    fun hashSuffix(
        useOldMangleRules: Boolean,
        valueParameters: List<IrType>,
        returnType: IrType?,
        addContinuation: Boolean = false
    ): String? =
        collectFunctionSignatureForManglingSuffix(
            useOldMangleRules,
            valueParameters.any { it.getRequiresMangling() },
            // The JVM backend computes mangled names after creating suspend function views, but before default argument
            // stub insertion. It would be nice if this part of the continuation lowering happened earlier in the pipeline.
            // TODO: Move suspend function view creation before JvmInlineClassLowering.
            if (addContinuation)
                valueParameters.map { it.asInfoForMangling() } +
                        InfoForMangling(FqNameUnsafe("kotlin.coroutines.Continuation"), isInline = false, isNullable = false)
            else
                valueParameters.map { it.asInfoForMangling() },
            returnType?.asInfoForMangling()
        )?.let(::md5base64)

    private fun IrType.asInfoForMangling(): InfoForMangling =
        InfoForMangling(
            erasedUpperBound.fqNameWhenAvailable!!.toUnsafe(),
            isInline = isInlineClassType(),
            isNullable = isNullable()
        )

    private val IrFunction.propertyName: Name
        get() = (this as IrSimpleFunction).correspondingPropertySymbol!!.owner.name
}

fun IrType.getRequiresMangling(includeInline: Boolean = true, includeMFVC: Boolean = true): Boolean {
    val irClass = erasedUpperBound
    return irClass.fqNameWhenAvailable != StandardNames.RESULT_FQ_NAME && when {
        irClass.isSingleFieldValueClass -> includeInline
        irClass.isMultiFieldValueClass -> includeMFVC
        else -> false
    }
}

val IrFunction.fullValueParameterList: List<IrValueParameter>
    get() = listOfNotNull(extensionReceiverParameter) + valueParameters

fun IrFunction.hasMangledParameters(includeInline: Boolean = true, includeMFVC: Boolean = true): Boolean =
    (dispatchReceiverParameter != null && when {
        parentAsClass.isSingleFieldValueClass -> includeInline
        parentAsClass.isMultiFieldValueClass -> includeMFVC
        else -> false
    }) || fullValueParameterList.any { it.type.getRequiresMangling(includeInline, includeMFVC) } || (this is IrConstructor && when {
        constructedClass.isSingleFieldValueClass -> includeInline
        constructedClass.isMultiFieldValueClass -> includeMFVC
        else -> false
    })

val IrFunction.hasMangledReturnType: Boolean
    get() = returnType.isInlineClassType() && parentClassOrNull?.isFileClass != true

val IrClass.inlineClassFieldName: Name
    get() = (inlineClassRepresentation ?: error("Not an inline class: ${render()}")).underlyingPropertyName

val IrFunction.isInlineClassFieldGetter: Boolean
    get() = (parent as? IrClass)?.isSingleFieldValueClass == true && this is IrSimpleFunction && extensionReceiverParameter == null &&
            contextReceiverParametersCount == 0 && !isStatic &&
            correspondingPropertySymbol?.let { it.owner.getter == this && it.owner.name == parentAsClass.inlineClassFieldName } == true

val IrFunction.isMultiFieldValueClassFieldGetter: Boolean
    get() = (parent as? IrClass)?.isMultiFieldValueClass == true && this is IrSimpleFunction && extensionReceiverParameter == null &&
            contextReceiverParametersCount == 0 && !isStatic &&
            correspondingPropertySymbol?.let {
                val multiFieldValueClassRepresentation = parentAsClass.multiFieldValueClassRepresentation
                    ?: error("Multi-field value class must have multiFieldValueClassRepresentation: ${parentAsClass.render()}")
                it.owner.getter == this && multiFieldValueClassRepresentation.containsPropertyWithName(it.owner.name)
            } == true
