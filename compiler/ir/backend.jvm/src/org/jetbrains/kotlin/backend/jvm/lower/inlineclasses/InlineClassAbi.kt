/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.inlineclasses

import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
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
    fun mangledNameFor(irFunction: IrFunction, mangleReturnTypes: Boolean, useOldMangleRules: Boolean): Name {
        if (irFunction is IrConstructor) {
            // Note that we might drop this convention and use standard mangling for constructors too, see KT-37186.
            assert(irFunction.constructedClass.isInline) {
                "Should not mangle names of non-inline class constructors: ${irFunction.render()}"
            }
            return Name.identifier("constructor-impl")
        }

        val suffix = hashSuffix(irFunction, mangleReturnTypes, useOldMangleRules) ?: return irFunction.name

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

        return Name.identifier("$base-$suffix")
    }

    private fun hashSuffix(
        irFunction: IrFunction,
        mangleReturnTypes: Boolean,
        useOldMangleRules: Boolean,
        alwaysMangleReturnType: Boolean = false
    ): String? {
        val signatureForMangling = collectFunctionSignatureForManglingSuffix(
            useOldManglingRules = useOldMangleRules,
            requiresFunctionNameManglingForParameterTypes = irFunction.fullValueParameterList.any { it.type.requiresMangling },
            fqNamesForMangling = irFunction.fullValueParameterList.map {
                it.type.asInfoForMangling()
            } + listOfNotNull(
                // The JVM backend computes mangled names after creating suspend function views, but before default argument
                // stub insertion. It would be nice if this part of the continuation lowering happened earlier in the pipeline.
                // TODO: Move suspend function view creation before JvmInlineClassLowering.
                if (irFunction.isSuspend)
                    InfoForMangling(FqNameUnsafe("kotlin.coroutines.Continuation"), isInline = false, isNullable = false)
                else null
            ),
            returnTypeInfo = if (alwaysMangleReturnType || (mangleReturnTypes && irFunction.hasMangledReturnType)) {
                irFunction.returnType.asInfoForMangling()
            } else null
        )

        return when {
            signatureForMangling != null -> md5base64(signatureForMangling)
            (irFunction.parent as? IrClass)?.isInline == true && irFunction.origin != IrDeclarationOrigin.IR_BUILTINS_STUB -> "impl"
            else -> null
        }
    }

    fun hashReturnSuffix(irFunction: IrFunction): String =
        hashSuffix(
            irFunction,
            mangleReturnTypes = true,
            useOldMangleRules = false,
            alwaysMangleReturnType = true
        )!!

    private fun IrType.asInfoForMangling(): InfoForMangling =
        InfoForMangling(
            erasedUpperBound.fqNameWhenAvailable!!.toUnsafe(),
            isInline = getClass()?.isInline == true,
            isNullable = isNullable()
        )

    private val IrFunction.propertyName: Name
        get() = (this as IrSimpleFunction).correspondingPropertySymbol!!.owner.name
}

internal val IrType.requiresMangling: Boolean
    get() {
        val irClass = erasedUpperBound
        return irClass.isInline && irClass.fqNameWhenAvailable != StandardNames.RESULT_FQ_NAME
    }

internal val IrFunction.fullValueParameterList: List<IrValueParameter>
    get() = listOfNotNull(extensionReceiverParameter) + valueParameters

internal val IrFunction.hasMangledParameters: Boolean
    get() = dispatchReceiverParameter != null && parentAsClass.isInline ||
            fullValueParameterList.any { it.type.requiresMangling } ||
            (this is IrConstructor && constructedClass.isInline)

internal val IrFunction.hasMangledReturnType: Boolean
    get() = returnType.erasedUpperBound.isInline && parentClassOrNull?.isFileClass != true

internal val IrClass.inlineClassFieldName: Name
    get() = primaryConstructor!!.valueParameters.single().name

val IrFunction.isInlineClassFieldGetter: Boolean
    get() = (parent as? IrClass)?.isInline == true && this is IrSimpleFunction && extensionReceiverParameter == null &&
            correspondingPropertySymbol?.let { it.owner.getter == this && it.owner.name == parentAsClass.inlineClassFieldName } == true

val IrFunction.isPrimaryInlineClassConstructor: Boolean
    get() = this is IrConstructor && isPrimary && constructedClass.isInline
