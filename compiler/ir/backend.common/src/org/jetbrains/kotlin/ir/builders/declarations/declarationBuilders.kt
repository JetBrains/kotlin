/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.types.Variance

fun IrClassBuilder.buildClass(): IrClass {
    val wrappedDescriptor = WrappedClassDescriptor()
    return IrClassImpl(
        startOffset, endOffset, origin,
        IrClassSymbolImpl(wrappedDescriptor),
        name, kind, visibility, modality,
        isCompanion = isCompanion, isInner = isInner, isData = isData, isExternal = isExternal, isInline = isInline, isExpect = isExpect
    ).also {
        wrappedDescriptor.bind(it)
    }
}

inline fun buildClass(builder: IrClassBuilder.() -> Unit) =
    IrClassBuilder().run {
        builder()
        buildClass()
    }


fun IrFieldBuilder.buildField(): IrField {
    val wrappedDescriptor = WrappedFieldDescriptor()
    return IrFieldImpl(
        startOffset, endOffset, origin,
        IrFieldSymbolImpl(wrappedDescriptor),
        name, type, visibility, isFinal, isExternal, isStatic,
        origin == IrDeclarationOrigin.FAKE_OVERRIDE
    ).also {
        wrappedDescriptor.bind(it)
    }
}

inline fun buildField(builder: IrFieldBuilder.() -> Unit) =
    IrFieldBuilder().run {
        builder()
        buildField()
    }

inline fun IrDeclarationContainer.addField(builder: IrFieldBuilder.() -> Unit) =
    buildField(builder).also { field ->
        field.parent = this
        declarations.add(field)
    }

fun IrClass.addField(fieldName: String, fieldType: IrType, fieldVisibility: Visibility = Visibilities.PRIVATE): IrField =
    addField {
        name = Name.identifier(fieldName)
        type = fieldType
        visibility = fieldVisibility
    }

fun IrPropertyBuilder.buildProperty(): IrProperty {
    val wrappedDescriptor = WrappedPropertyDescriptor()
    return IrPropertyImpl(
        startOffset, endOffset, origin,
        IrPropertySymbolImpl(wrappedDescriptor),
        name, visibility, modality,
        isVar = isVar, isConst = isConst, isLateinit = isLateinit, isDelegated = isDelegated, isExpect = isExpect, isExternal = isExternal,
        isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE
    ).also {
        wrappedDescriptor.bind(it)
    }
}

inline fun buildProperty(builder: IrPropertyBuilder.() -> Unit) =
    IrPropertyBuilder().run {
        builder()
        buildProperty()
    }

inline fun IrDeclarationContainer.addProperty(builder: IrPropertyBuilder.() -> Unit): IrProperty =
    buildProperty(builder).also { property ->
        declarations.add(property)
        property.parent = this@addProperty
    }

inline fun IrProperty.addGetter(builder: IrFunctionBuilder.() -> Unit = {}): IrSimpleFunction =
    IrFunctionBuilder().run {
        name = Name.special("<get-${this@addGetter.name}>")
        builder()
        buildFun().also { getter ->
            this@addGetter.getter = getter
            getter.parent = this@addGetter.parent
        }
    }

inline fun IrProperty.addSetter(builder: IrFunctionBuilder.() -> Unit = {}): IrSimpleFunction =
    IrFunctionBuilder().run {
        name = Name.special("<set-${this@addSetter.name}>")
        builder()
        buildFun().also { setter ->
            this@addSetter.setter = setter
            setter.parent = this@addSetter.parent
        }
    }

fun IrFunctionBuilder.buildFun(originalDescriptor: FunctionDescriptor? = null): IrFunctionImpl {
    val wrappedDescriptor = if (originalDescriptor is DescriptorWithContainerSource)
        WrappedFunctionDescriptorWithContainerSource(originalDescriptor.containerSource)
    else if (originalDescriptor != null)
        WrappedSimpleFunctionDescriptor(originalDescriptor)
    else
        WrappedSimpleFunctionDescriptor()
    return IrFunctionImpl(
        startOffset, endOffset, origin,
        IrSimpleFunctionSymbolImpl(wrappedDescriptor),
        name, visibility, modality, returnType,
        isInline = isInline, isExternal = isExternal, isTailrec = isTailrec, isSuspend = isSuspend, isExpect = isExpect,
        isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE
    ).also {
        wrappedDescriptor.bind(it)
    }
}

fun IrFunctionBuilder.buildConstructor(): IrConstructorImpl {
    val wrappedDescriptor = WrappedClassConstructorDescriptor()
    return IrConstructorImpl(
        startOffset, endOffset, origin,
        IrConstructorSymbolImpl(wrappedDescriptor),
        Name.special("<init>"),
        visibility, returnType,
        isInline = isInline, isExternal = isExternal, isPrimary = isPrimary, isExpect = isExpect
    ).also {
        wrappedDescriptor.bind(it)
    }
}

/**
 * Inlining relies on descriptors for external declarations. When replacing a
 * potentially external function (e.g. in an IrCall) we have to ensure that we keep
 * information from the original descriptor so as not to break inlining.
 */
inline fun buildFunWithDescriptorForInlining(originalDescriptor: FunctionDescriptor, builder: IrFunctionBuilder.() -> Unit): IrFunctionImpl =
    IrFunctionBuilder().run {
        builder()
        buildFun(originalDescriptor)
    }

inline fun buildFun(builder: IrFunctionBuilder.() -> Unit): IrFunctionImpl =
    IrFunctionBuilder().run {
        builder()
        buildFun()
    }

inline fun IrDeclarationContainer.addFunction(builder: IrFunctionBuilder.() -> Unit): IrFunctionImpl =
    buildFun(builder).also { function ->
        declarations.add(function)
        function.parent = this@addFunction
    }

fun IrDeclarationContainer.addFunction(
    name: String,
    returnType: IrType,
    modality: Modality = Modality.FINAL,
    isStatic: Boolean = false,
    isSuspend: Boolean = false
): IrSimpleFunction =
    addFunction {
        this.name = Name.identifier(name)
        this.returnType = returnType
        this.modality = modality
        this.isSuspend = isSuspend
    }.apply {
        if (!isStatic) {
            dispatchReceiverParameter = parentAsClass.thisReceiver!!.copyTo(this)
        }
    }

inline fun buildConstructor(builder: IrFunctionBuilder.() -> Unit): IrConstructorImpl =
    IrFunctionBuilder().run {
        builder()
        buildConstructor()
    }

inline fun IrClass.addConstructor(builder: IrFunctionBuilder.() -> Unit = {}): IrConstructorImpl =
    buildConstructor {
        builder()
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

inline fun buildValueParameter(builder: IrValueParameterBuilder.() -> Unit): IrValueParameter =
    IrValueParameterBuilder().run {
        builder()
        build()
    }

inline fun IrFunction.addValueParameter(builder: IrValueParameterBuilder.() -> Unit): IrValueParameter =
    IrValueParameterBuilder().run {
        builder()
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

inline fun IrSimpleFunction.addDispatchReceiver(builder: IrValueParameterBuilder.() -> Unit): IrValueParameter =
    IrValueParameterBuilder().run {
        builder()
        index = -1
        name = "this".synthesizedName
        build().also { receiver ->
            dispatchReceiverParameter = receiver
            receiver.parent = this@addDispatchReceiver
        }
    }

inline fun IrSimpleFunction.addExtensionReceiver(builder: IrValueParameterBuilder.() -> Unit): IrValueParameter =
    IrValueParameterBuilder().run {
        builder()
        index = -1
        name = "receiver".synthesizedName
        build().also { receiver ->
            extensionReceiverParameter = receiver
            receiver.parent = this@addExtensionReceiver
        }
    }

fun IrSimpleFunction.addExtensionReceiver(type: IrType, origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED ): IrValueParameter =
    addExtensionReceiver {
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

inline fun IrTypeParametersContainer.addTypeParameter(builder: IrTypeParameterBuilder.() -> Unit): IrTypeParameter =
    IrTypeParameterBuilder().run {
        builder()
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
