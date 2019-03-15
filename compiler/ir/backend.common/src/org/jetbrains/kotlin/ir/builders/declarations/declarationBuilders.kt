/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

fun IrClassBuilder.buildClass(): IrClass {
    val wrappedDescriptor = WrappedClassDescriptor()
    return IrClassImpl(
        startOffset, endOffset, origin,
        IrClassSymbolImpl(wrappedDescriptor),
        name, kind, visibility, modality, isCompanion, isInner, isData, isExternal, isInline
    ).also {
        wrappedDescriptor.bind(it)
    }
}

inline fun buildClass(b: IrClassBuilder.() -> Unit) =
    IrClassBuilder().run {
        b()
        buildClass()
    }


fun IrFieldBuilder.buildField(): IrField {
    val wrappedDescriptor = WrappedFieldDescriptor()
    return IrFieldImpl(
        startOffset, endOffset, origin,
        IrFieldSymbolImpl(wrappedDescriptor),
        name, type, visibility, isFinal, isExternal, isStatic
    ).also {
        wrappedDescriptor.bind(it)
    }
}

inline fun buildField(b: IrFieldBuilder.() -> Unit) =
    IrFieldBuilder().run {
        b()
        buildField()
    }

fun IrFunctionBuilder.buildFun(): IrSimpleFunction {
    val wrappedDescriptor = WrappedSimpleFunctionDescriptor()
    return IrFunctionImpl(
        startOffset, endOffset, origin,
        IrSimpleFunctionSymbolImpl(wrappedDescriptor),
        name, visibility, modality, returnType,
        isInline = isInline, isExternal = isExternal, isTailrec = isTailrec, isSuspend = isSuspend
    ).also {
        wrappedDescriptor.bind(it)
    }
}

fun IrFunctionBuilder.buildConstructor(): IrConstructor {
    val wrappedDescriptor = WrappedClassConstructorDescriptor()
    return IrConstructorImpl(
        startOffset, endOffset, origin,
        IrConstructorSymbolImpl(wrappedDescriptor),
        Name.special("<init>"),
        visibility, returnType,
        isInline = isInline, isExternal = isExternal, isPrimary = isPrimary
    ).also {
        wrappedDescriptor.bind(it)
    }
}

inline fun buildFun(b: IrFunctionBuilder.() -> Unit): IrSimpleFunction =
    IrFunctionBuilder().run {
        b()
        buildFun()
    }

inline fun buildConstructor(b: IrFunctionBuilder.() -> Unit): IrConstructor =
    IrFunctionBuilder().run {
        b()
        buildConstructor()
    }

fun IrValueParameterBuilder.build(): IrValueParameter {
    val wrappedDescriptor = WrappedValueParameterDescriptor()
    return IrValueParameterImpl(
        startOffset, endOffset, origin,
        IrValueParameterSymbolImpl(wrappedDescriptor),
        name, index, type, varargElementType, isCrossInline, isNoinline
    ).also {
        wrappedDescriptor.bind(it)
    }
}

inline fun buildValueParameter(b: IrValueParameterBuilder.() -> Unit): IrValueParameter =
    IrValueParameterBuilder().run {
        b()
        build()
    }

inline fun IrFunction.addValueParameter(b: IrValueParameterBuilder.() -> Unit): IrValueParameter =
    IrValueParameterBuilder().run {
        b()
        if (index == UNDEFINED_PARAMETER_INDEX) {
            index = valueParameters.size
        }
        build().also { valueParameter ->
            valueParameters.add(valueParameter)
            valueParameter.parent = this@addValueParameter
        }
    }

fun IrFunction.addValueParameter(name: Name, type: IrType, origin: IrDeclarationOrigin): IrValueParameter =
    addValueParameter {
        this.name = name
        this.type = type
        this.origin = origin
    }