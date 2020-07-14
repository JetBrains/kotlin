/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.types.Variance

@PublishedApi
internal fun IrFactory.buildClass(builder: IrClassBuilder): IrClass = with(builder) {
    val wrappedDescriptor = WrappedClassDescriptor()
    createClass(
        startOffset, endOffset, origin,
        IrClassSymbolImpl(wrappedDescriptor),
        name, kind, visibility, modality,
        isCompanion, isInner, isData, isExternal, isInline, isExpect, isFun
    ).also {
        wrappedDescriptor.bind(it)
    }
}

inline fun IrFactory.buildClass(builder: IrClassBuilder.() -> Unit) =
    IrClassBuilder().run {
        builder()
        buildClass(this)
    }

@PublishedApi
internal fun IrFactory.buildField(builder: IrFieldBuilder): IrField = with(builder) {
    val wrappedDescriptor = WrappedFieldDescriptor()
    createField(
        startOffset, endOffset, origin,
        IrFieldSymbolImpl(wrappedDescriptor),
        name, type, visibility, isFinal, isExternal, isStatic,
    ).also {
        it.metadata = metadata
        wrappedDescriptor.bind(it)
    }
}

inline fun IrFactory.buildField(builder: IrFieldBuilder.() -> Unit) =
    IrFieldBuilder().run {
        builder()
        buildField(this)
    }

inline fun IrClass.addField(builder: IrFieldBuilder.() -> Unit) =
    factory.buildField(builder).also { field ->
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
internal fun IrFactory.buildProperty(
    builder: IrPropertyBuilder,
    originalDescriptor: PropertyDescriptor? = null
): IrProperty = with(builder) {
    val wrappedDescriptor = when (originalDescriptor) {
        is DescriptorWithContainerSource -> WrappedPropertyDescriptorWithContainerSource(originalDescriptor.containerSource)
        else -> WrappedPropertyDescriptor()
    }
    createProperty(
        startOffset, endOffset, origin,
        IrPropertySymbolImpl(wrappedDescriptor),
        name, visibility, modality,
        isVar, isConst, isLateinit, isDelegated, isExternal, isExpect, isFakeOverride
    ).also {
        wrappedDescriptor.bind(it)
    }
}

inline fun IrFactory.buildProperty(originalDescriptor: PropertyDescriptor? = null, builder: IrPropertyBuilder.() -> Unit) =
    IrPropertyBuilder().run {
        builder()
        buildProperty(this, originalDescriptor)
    }

inline fun IrClass.addProperty(originalDescriptor: PropertyDescriptor? = null, builder: IrPropertyBuilder.() -> Unit): IrProperty =
    factory.buildProperty(originalDescriptor, builder).also { property ->
        declarations.add(property)
        property.parent = this@addProperty
    }

inline fun IrProperty.addGetter(builder: IrFunctionBuilder.() -> Unit = {}): IrSimpleFunction =
    IrFunctionBuilder().run {
        name = Name.special("<get-${this@addGetter.name}>")
        builder()
        factory.buildFunction(this).also { getter ->
            this@addGetter.getter = getter
            getter.correspondingPropertySymbol = this@addGetter.symbol
            getter.parent = this@addGetter.parent
        }
    }

@PublishedApi
internal fun IrFactory.buildFunction(
    builder: IrFunctionBuilder,
    originalDescriptor: FunctionDescriptor? = null
): IrSimpleFunction = with(builder) {
    // Inlining relies on descriptors for external declarations. When replacing a potentially external function (e.g. in an IrCall),
    // we have to ensure that we keep information from the original descriptor so as not to break inlining.
    val wrappedDescriptor = when (originalDescriptor) {
        is DescriptorWithContainerSource -> WrappedFunctionDescriptorWithContainerSource(originalDescriptor.containerSource)
        is PropertyGetterDescriptor -> WrappedPropertyGetterDescriptor(originalDescriptor.annotations, originalDescriptor.source)
        is PropertySetterDescriptor -> WrappedPropertySetterDescriptor(originalDescriptor.annotations, originalDescriptor.source)
        null -> WrappedSimpleFunctionDescriptor()
        else -> WrappedSimpleFunctionDescriptor(originalDescriptor)
    }
    createFunction(
        startOffset, endOffset, origin,
        IrSimpleFunctionSymbolImpl(wrappedDescriptor),
        name, visibility, modality, returnType,
        isInline, isExternal, isTailrec, isSuspend, isOperator, isInfix, isExpect, isFakeOverride
    ).also {
        wrappedDescriptor.bind(it)
    }
}

@PublishedApi
internal fun IrFactory.buildConstructor(
    builder: IrFunctionBuilder, originalDescriptor: ConstructorDescriptor?
): IrConstructor = with(builder) {
    val wrappedDescriptor =
        if (originalDescriptor != null) WrappedClassConstructorDescriptor(originalDescriptor.annotations, originalDescriptor.source)
        else WrappedClassConstructorDescriptor()
    return createConstructor(
        startOffset, endOffset, origin,
        IrConstructorSymbolImpl(wrappedDescriptor),
        Name.special("<init>"),
        visibility, returnType,
        isInline = isInline, isExternal = isExternal, isPrimary = isPrimary, isExpect = isExpect
    ).also {
        wrappedDescriptor.bind(it)
    }
}

inline fun IrFactory.buildFun(
    originalDescriptor: FunctionDescriptor? = null, builder: IrFunctionBuilder.() -> Unit
): IrSimpleFunction =
    IrFunctionBuilder().run {
        builder()
        buildFunction(this, originalDescriptor)
    }

inline fun IrFactory.addFunction(klass: IrDeclarationContainer, builder: IrFunctionBuilder.() -> Unit): IrSimpleFunction =
    buildFun(null, builder).also { function ->
        klass.declarations.add(function)
        function.parent = klass
    }

inline fun IrClass.addFunction(builder: IrFunctionBuilder.() -> Unit): IrSimpleFunction =
    factory.addFunction(this, builder)

fun IrClass.addFunction(
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

inline fun IrFactory.buildConstructor(
    originalDescriptor: ConstructorDescriptor? = null,
    builder: IrFunctionBuilder.() -> Unit
): IrConstructor =
    IrFunctionBuilder().run {
        builder()
        buildConstructor(this, originalDescriptor)
    }

inline fun IrClass.addConstructor(builder: IrFunctionBuilder.() -> Unit = {}): IrConstructor =
    factory.buildConstructor {
        builder()
        returnType = defaultType
    }.also { constructor ->
        declarations.add(constructor)
        constructor.parent = this@addConstructor
    }

private val RECEIVER_PARAMETER_NAME = Name.special("<this>")

fun <D> buildReceiverParameter(
    parent: D,
    origin: IrDeclarationOrigin,
    type: IrType,
    startOffset: Int = parent.startOffset,
    endOffset: Int = parent.endOffset
): IrValueParameter
    where D : IrDeclaration, D : IrDeclarationParent = WrappedReceiverParameterDescriptor().let { wrappedDescriptor ->
    parent.factory.createValueParameter(
        startOffset, endOffset, origin,
        IrValueParameterSymbolImpl(wrappedDescriptor),
        RECEIVER_PARAMETER_NAME, -1, type, null, isCrossinline = false, isNoinline = false
    ).also {
        wrappedDescriptor.bind(it)
        it.parent = parent
    }
}

@PublishedApi
internal fun IrFactory.buildValueParameter(builder: IrValueParameterBuilder, parent: IrDeclarationParent): IrValueParameter =
    with(builder) {
        val wrappedDescriptor = WrappedValueParameterDescriptor(wrappedDescriptorAnnotations)
        return createValueParameter(
            startOffset, endOffset, origin,
            IrValueParameterSymbolImpl(wrappedDescriptor),
            name, index, type, varargElementType, isCrossInline, isNoinline
        ).also {
            wrappedDescriptor.bind(it)
            it.parent = parent
        }
    }


inline fun <D> buildValueParameter(declaration: D, builder: IrValueParameterBuilder.() -> Unit): IrValueParameter
    where D : IrDeclaration, D : IrDeclarationParent =
    IrValueParameterBuilder().run {
        builder()
        declaration.factory.buildValueParameter(this, declaration)
    }

inline fun IrFunction.addValueParameter(builder: IrValueParameterBuilder.() -> Unit): IrValueParameter =
    IrValueParameterBuilder().run {
        builder()
        if (index == UNDEFINED_PARAMETER_INDEX) {
            index = valueParameters.size
        }
        factory.buildValueParameter(this, this@addValueParameter).also { valueParameter ->
            valueParameters = valueParameters + valueParameter
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
        factory.buildValueParameter(this, this@addDispatchReceiver).also { receiver ->
            dispatchReceiverParameter = receiver
        }
    }

fun IrSimpleFunction.addExtensionReceiver(type: IrType, origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED): IrValueParameter =
    IrValueParameterBuilder().run {
        this.type = type
        this.origin = origin
        this.index = -1
        this.name = "receiver".synthesizedName
        factory.buildValueParameter(this, this@addExtensionReceiver).also { receiver ->
            extensionReceiverParameter = receiver
        }
    }

@PublishedApi
internal fun IrFactory.buildTypeParameter(builder: IrTypeParameterBuilder, parent: IrDeclarationParent): IrTypeParameter =
    with(builder) {
        val wrappedDescriptor = WrappedTypeParameterDescriptor()
        createTypeParameter(
            startOffset, endOffset, origin,
            IrTypeParameterSymbolImpl(wrappedDescriptor),
            name, index, isReified, variance
        ).also {
            wrappedDescriptor.bind(it)
            it.superTypes.addAll(superTypes)
            it.parent = parent
        }
    }

inline fun buildTypeParameter(parent: IrTypeParametersContainer, builder: IrTypeParameterBuilder.() -> Unit): IrTypeParameter =
    IrTypeParameterBuilder().run {
        builder()
        parent.factory.buildTypeParameter(this, parent)
    }

inline fun IrTypeParametersContainer.addTypeParameter(builder: IrTypeParameterBuilder.() -> Unit): IrTypeParameter =
    IrTypeParameterBuilder().run {
        builder()
        if (index == UNDEFINED_PARAMETER_INDEX) {
            index = typeParameters.size
        }
        factory.buildTypeParameter(this, this@addTypeParameter).also { typeParameter ->
            typeParameters = typeParameters + typeParameter
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
