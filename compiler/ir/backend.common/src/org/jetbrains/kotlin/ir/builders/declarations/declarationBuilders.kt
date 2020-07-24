/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.descriptors.*
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

@PublishedApi
internal fun IrClassBuilder.buildClass(): IrClass {
    val wrappedDescriptor = WrappedClassDescriptor()
    return IrClassImpl(
        startOffset, endOffset, origin,
        IrClassSymbolImpl(wrappedDescriptor),
        name, kind, visibility, modality,
        isCompanion, isInner, isData, isExternal, isInline, isExpect, isFun
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
    ).also {
        it.metadata = metadata
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

fun IrClass.addField(fieldName: Name, fieldType: IrType, fieldVisibility: Visibility = Visibilities.PRIVATE): IrField =
    addField {
        name = fieldName
        type = fieldType
        visibility = fieldVisibility
    }

fun IrClass.addField(fieldName: String, fieldType: IrType, fieldVisibility: Visibility = Visibilities.PRIVATE): IrField =
    addField(Name.identifier(fieldName), fieldType, fieldVisibility)

@PublishedApi
internal fun IrPropertyBuilder.buildProperty(originalDescriptor: PropertyDescriptor? = null): IrProperty {
    val wrappedDescriptor = when (originalDescriptor) {
        is DescriptorWithContainerSource -> WrappedPropertyDescriptorWithContainerSource(originalDescriptor.containerSource)
        else -> WrappedPropertyDescriptor()
    }
    return IrPropertyImpl(
        startOffset, endOffset, origin,
        IrPropertySymbolImpl(wrappedDescriptor),
        name, visibility, modality,
        isVar, isConst, isLateinit, isDelegated, isExternal, isExpect, isFakeOverride
    ).also {
        wrappedDescriptor.bind(it)
    }
}

inline fun buildProperty(originalDescriptor: PropertyDescriptor? = null, builder: IrPropertyBuilder.() -> Unit) =
    IrPropertyBuilder().run {
        builder()
        buildProperty(originalDescriptor)
    }

inline fun IrDeclarationContainer.addProperty(originalDescriptor: PropertyDescriptor? = null, builder: IrPropertyBuilder.() -> Unit): IrProperty =
    buildProperty(originalDescriptor, builder).also { property ->
        declarations.add(property)
        property.parent = this@addProperty
    }

inline fun IrProperty.addGetter(builder: IrFunctionBuilder.() -> Unit = {}): IrSimpleFunction =
    IrFunctionBuilder().run {
        name = Name.special("<get-${this@addGetter.name}>")
        builder()
        buildFunction().also { getter ->
            this@addGetter.getter = getter
            getter.correspondingPropertySymbol = this@addGetter.symbol
            getter.parent = this@addGetter.parent
        }
    }

inline fun IrProperty.addSetter(builder: IrFunctionBuilder.() -> Unit = {}): IrSimpleFunction =
    IrFunctionBuilder().run {
        name = Name.special("<set-${this@addSetter.name}>")
        builder()
        buildFunction().also { setter ->
            this@addSetter.setter = setter
            setter.parent = this@addSetter.parent
        }
    }

@PublishedApi
internal fun IrFunctionBuilder.buildFunction(originalDescriptor: FunctionDescriptor? = null): IrSimpleFunction {
    // Inlining relies on descriptors for external declarations. When replacing a potentially external function (e.g. in an IrCall),
    // we have to ensure that we keep information from the original descriptor so as not to break inlining.
    val wrappedDescriptor = when (originalDescriptor) {
        is DescriptorWithContainerSource -> WrappedFunctionDescriptorWithContainerSource(originalDescriptor.containerSource)
        is PropertyGetterDescriptor -> WrappedPropertyGetterDescriptor(originalDescriptor.annotations, originalDescriptor.source)
        is PropertySetterDescriptor -> WrappedPropertySetterDescriptor(originalDescriptor.annotations, originalDescriptor.source)
        null -> WrappedSimpleFunctionDescriptor()
        else -> WrappedSimpleFunctionDescriptor(originalDescriptor)
    }
    return IrFunctionImpl(
        startOffset, endOffset, origin,
        IrSimpleFunctionSymbolImpl(wrappedDescriptor),
        name, visibility, modality, returnType,
        isInline, isExternal, isTailrec, isSuspend, isOperator, isInfix, isExpect, isFakeOverride
    ).also {
        wrappedDescriptor.bind(it)
    }
}

@PublishedApi
internal fun IrFunctionBuilder.buildConstructor(originalDescriptor: ConstructorDescriptor?): IrConstructor {
    val wrappedDescriptor =
        if (originalDescriptor != null) WrappedClassConstructorDescriptor(originalDescriptor.annotations, originalDescriptor.source)
        else WrappedClassConstructorDescriptor()
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

inline fun buildFun(originalDescriptor: FunctionDescriptor? = null, builder: IrFunctionBuilder.() -> Unit): IrSimpleFunction =
    IrFunctionBuilder().run {
        builder()
        buildFunction(originalDescriptor)
    }

inline fun IrDeclarationContainer.addFunction(builder: IrFunctionBuilder.() -> Unit): IrSimpleFunction =
    buildFun(null, builder).also { function ->
        declarations.add(function)
        function.parent = this@addFunction
    }

fun IrDeclarationContainer.addFunction(
    name: String,
    returnType: IrType,
    modality: Modality = Modality.FINAL,
    visibility: Visibility = Visibilities.PUBLIC,
    isStatic: Boolean = false,
    isSuspend: Boolean = false,
    isFakeOverride: Boolean = false,
    origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
): IrSimpleFunction =
    addFunction {
        this.name = Name.identifier(name)
        this.returnType = returnType
        this.modality = modality
        this.visibility = visibility
        this.isSuspend = isSuspend
        this.isFakeOverride = isFakeOverride
        this.origin = origin
    }.apply {
        if (!isStatic) {
            dispatchReceiverParameter = parentAsClass.thisReceiver!!.copyTo(this)
        }
    }

inline fun buildConstructor(originalDescriptor: ConstructorDescriptor? = null, builder: IrFunctionBuilder.() -> Unit): IrConstructor =
    IrFunctionBuilder().run {
        builder()
        buildConstructor(originalDescriptor)
    }

inline fun IrClass.addConstructor(builder: IrFunctionBuilder.() -> Unit = {}): IrConstructor =
    buildConstructor {
        builder()
        returnType = defaultType
    }.also { constructor ->
        declarations.add(constructor)
        constructor.parent = this@addConstructor
    }

private val RECEIVER_PARAMETER_NAME = Name.special("<this>")

fun buildReceiverParameter(
    parent: IrDeclarationParent,
    origin: IrDeclarationOrigin,
    type: IrType,
    startOffset: Int = parent.startOffset,
    endOffset: Int = parent.endOffset
): IrValueParameter = WrappedReceiverParameterDescriptor().let { wrappedDescriptor ->
    IrValueParameterImpl(
        startOffset, endOffset, origin,
        IrValueParameterSymbolImpl(wrappedDescriptor),
        RECEIVER_PARAMETER_NAME, -1, type, null, isCrossinline = false, isNoinline = false
    ).also {
        wrappedDescriptor.bind(it)
        it.parent = parent
    }
}

@PublishedApi
internal fun IrValueParameterBuilder.buildValueParameter(parent: IrDeclarationParent): IrValueParameter {
    val wrappedDescriptor = WrappedValueParameterDescriptor(wrappedDescriptorAnnotations)
    return IrValueParameterImpl(
        startOffset, endOffset, origin,
        IrValueParameterSymbolImpl(wrappedDescriptor),
        name, index, type, varargElementType, isCrossInline, isNoinline
    ).also {
        wrappedDescriptor.bind(it)
        it.parent = parent
    }
}

inline fun buildValueParameter(parent: IrDeclarationParent, builder: IrValueParameterBuilder.() -> Unit): IrValueParameter =
    IrValueParameterBuilder().run {
        builder()
        buildValueParameter(parent)
    }

inline fun IrFunction.addValueParameter(builder: IrValueParameterBuilder.() -> Unit): IrValueParameter =
    IrValueParameterBuilder().run {
        builder()
        if (index == UNDEFINED_PARAMETER_INDEX) {
            index = valueParameters.size
        }
        buildValueParameter(this@addValueParameter).also { valueParameter ->
            valueParameters += valueParameter
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
        buildValueParameter(this@addDispatchReceiver).also { receiver ->
            dispatchReceiverParameter = receiver
        }
    }

inline fun IrSimpleFunction.addExtensionReceiver(builder: IrValueParameterBuilder.() -> Unit): IrValueParameter =
    IrValueParameterBuilder().run {
        builder()
        index = -1
        name = "receiver".synthesizedName
        buildValueParameter(this@addExtensionReceiver).also { receiver ->
            extensionReceiverParameter = receiver
        }
    }

fun IrSimpleFunction.addExtensionReceiver(type: IrType, origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED ): IrValueParameter =
    addExtensionReceiver {
        this.type = type
        this.origin = origin
    }

@PublishedApi
internal fun IrTypeParameterBuilder.buildTypeParameter(parent: IrDeclarationParent): IrTypeParameter {
    val wrappedDescriptor = WrappedTypeParameterDescriptor()
    return IrTypeParameterImpl(
        startOffset, endOffset, origin,
        IrTypeParameterSymbolImpl(wrappedDescriptor),
        name, index, isReified, variance
    ).also {
        wrappedDescriptor.bind(it)
        it.superTypes.addAll(superTypes)
        it.parent = parent
    }
}

inline fun buildTypeParameter(parent: IrDeclarationParent, builder: IrTypeParameterBuilder.() -> Unit): IrTypeParameter =
    IrTypeParameterBuilder().run {
        builder()
        buildTypeParameter(parent)
    }

inline fun IrTypeParametersContainer.addTypeParameter(builder: IrTypeParameterBuilder.() -> Unit): IrTypeParameter =
    IrTypeParameterBuilder().run {
        builder()
        if (index == UNDEFINED_PARAMETER_INDEX) {
            index = typeParameters.size
        }
        buildTypeParameter(this@addTypeParameter).also { typeParameter ->
            typeParameters += typeParameter
        }
    }

fun IrTypeParametersContainer.addTypeParameter(name: String, upperBound: IrType, variance: Variance = Variance.INVARIANT): IrTypeParameter =
    addTypeParameter {
        this.name = Name.identifier(name)
        this.variance = variance
        this.superTypes.add(upperBound)
    }

fun buildVariable(
    parent: IrDeclarationParent?,
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    name: Name,
    type: IrType,
    isVar: Boolean = false,
    isConst: Boolean = false,
    isLateinit: Boolean = false,
): IrVariable {
    val wrappedDescriptor = WrappedVariableDescriptor()
    return IrVariableImpl(
        startOffset, endOffset, origin,
        IrVariableSymbolImpl(wrappedDescriptor),
        name, type, isVar, isConst, isLateinit
    ).also {
        wrappedDescriptor.bind(it)
        if (parent != null) {
            it.parent = parent
        }
    }
}
