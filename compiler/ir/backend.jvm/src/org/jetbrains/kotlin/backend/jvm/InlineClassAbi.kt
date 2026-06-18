/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.ir.inlineClassRepresentation
import org.jetbrains.kotlin.backend.jvm.ir.isBasicValueClassType
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.backend.jvm.ir.isSingleFieldValueClass
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.state.InfoForMangling
import org.jetbrains.kotlin.codegen.state.collectFunctionSignatureForManglingSuffix
import org.jetbrains.kotlin.codegen.state.md5base64
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name

/**
 * Replace inline classes by their underlying types.
 */
fun IrType.unboxInlineClass() = InlineClassAbi.unboxType(this) ?: this

object InlineClassAbi {
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
            assert(irFunction.constructedClass.isBasicValueClass) {
                "Should not mangle names of non-inline class constructors: ${irFunction.render()}"
            }
            return Name.identifier("constructor-impl")
        }

        val suffix = hashSuffix(irFunction, mangleReturnTypes, useOldMangleRules)
        if (suffix == null && ((irFunction.parent as? IrClass)?.isBasicValueClass != true || irFunction.origin == IrDeclarationOrigin.IR_BUILTINS_STUB)) {
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

        return Name.identifier("$base-${suffix ?: "impl"}")
    }

    fun hashSuffix(irFunction: IrFunction, mangleReturnTypes: Boolean, useOldMangleRules: Boolean): String? =
        hashSuffix(
            useOldMangleRules,
            irFunction.nonDispatchParameters.map { it.type },
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
                        InfoForMangling(FqNameUnsafe("kotlin.coroutines.Continuation"), isValue = false, isNullable = false)
            else
                valueParameters.map { it.asInfoForMangling() },
            returnType?.asInfoForMangling()
        )?.let(::md5base64)

    private fun IrType.asInfoForMangling(): InfoForMangling =
        InfoForMangling(
            erasedUpperBound.fqNameWhenAvailable!!.toUnsafe(),
            isValue = isBasicValueClassType(),
            isNullable = isNullable()
        )

    private val IrFunction.propertyName: Name
        get() = (this as IrSimpleFunction).correspondingPropertySymbol!!.owner.name
}

fun IrType.getRequiresMangling(): Boolean {
    val irClass = erasedUpperBound
    return irClass.isSingleFieldValueClass && !irClass.isClassWithFqName(StandardNames.RESULT_FQ_NAME)
}

fun IrFunction.hasMangledParameters(): Boolean =
    (dispatchReceiverParameter != null && parentAsClass.isSingleFieldValueClass) ||
            nonDispatchParameters.any { it.type.getRequiresMangling() } ||
            (this is IrConstructor && constructedClass.isSingleFieldValueClass)

val IrFunction.hasMangledReturnType: Boolean
    get() = returnType.isInlineClassType() && parentClassOrNull?.isFileClass != true

val IrClass.inlineClassFieldName: Name
    get() = (inlineClassRepresentation ?: error("Not an inline class: ${render()}")).underlyingPropertyName

val IrFunction.isInlineClassFieldGetter: Boolean
    get() = (parent as? IrClass)?.isSingleFieldValueClass == true && this is IrSimpleFunction &&
            hasShape(dispatchReceiver = true) &&
            correspondingPropertySymbol?.let { it.owner.getter == this && it.owner.name == parentAsClass.inlineClassFieldName } == true
