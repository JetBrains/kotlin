/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

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

inline fun IrDeclarationContainer.addField(b: IrFieldBuilder.() -> Unit) =
    buildField(b).also { field ->
        field.parent = this
        declarations.add(field)
    }

fun IrPropertyBuilder.buildProperty(): IrProperty {
    val wrappedDescriptor = WrappedPropertyDescriptor()
    return IrPropertyImpl(
        startOffset, endOffset, origin,
        IrPropertySymbolImpl(wrappedDescriptor),
        name, visibility, modality, isVar, isConst, isLateinit, isDelegated, isExternal
    ).also {
        wrappedDescriptor.bind(it)
    }
}

inline fun buildProperty(b: IrPropertyBuilder.() -> Unit) =
    IrPropertyBuilder().run {
        b()
        buildProperty()
    }

inline fun IrDeclarationContainer.addProperty(b: IrPropertyBuilder.() -> Unit): IrProperty =
    buildProperty(b).also { property ->
        declarations.add(property)
        property.parent = this@addProperty
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

inline fun IrDeclarationContainer.addFunction(b: IrFunctionBuilder.() -> Unit): IrSimpleFunction =
    buildFun(b).also { function ->
        declarations.add(function)
        function.parent = this@addFunction
    }

fun IrDeclarationContainer.addFunction(
    name: String,
    returnType: IrType,
    modality: Modality = Modality.FINAL,
    isStatic: Boolean = false
): IrSimpleFunction =
    addFunction {
        this.name = Name.identifier(name)
        this.returnType = returnType
        this.modality = modality
    }.apply {
        if (!isStatic) {
            dispatchReceiverParameter = parentAsClass.thisReceiver!!.copyTo(this)
        }
    }

inline fun buildConstructor(b: IrFunctionBuilder.() -> Unit): IrConstructor =
    IrFunctionBuilder().run {
        b()
        buildConstructor()
    }

inline fun IrClass.addConstructor(b: IrFunctionBuilder.() -> Unit = {}): IrConstructor =
    buildConstructor {
        b()
        returnType = defaultType
    }.also { constructor ->
        declarations.add(constructor)
        constructor.parent = this@addConstructor
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

fun IrFunction.addValueParameter(name: String, type: IrType, origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED): IrValueParameter =
    addValueParameter {
        this.name = Name.identifier(name)
        this.type = type
        this.origin = origin
    }

fun IrTypeParameterBuilder.build(): IrTypeParameter {
    val wrappedDescriptor = WrappedTypeParameterDescriptor()
    return IrTypeParameterImpl(
        startOffset, endOffset, origin,
        IrTypeParameterSymbolImpl(wrappedDescriptor),
        name, index, isReified, variance
    ).also {
        wrappedDescriptor.bind(it)
        it.superTypes.addAll(superTypes)
    }
}

inline fun IrTypeParametersContainer.addTypeParameter(b: IrTypeParameterBuilder.() -> Unit): IrTypeParameter =
    IrTypeParameterBuilder().run {
        b()
        if (index == UNDEFINED_PARAMETER_INDEX) {
            index = typeParameters.size
        }
        build().also { typeParameter ->
            typeParameters.add(typeParameter)
            typeParameter.parent = this@addTypeParameter
        }
    }

fun IrTypeParametersContainer.addTypeParameter(name: String, upperBound: IrType, variance: Variance = Variance.INVARIANT): IrTypeParameter =
    addTypeParameter {
        this.name = Name.identifier(name)
        this.variance = variance
        this.superTypes.add(upperBound)
    }
