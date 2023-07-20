/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

@PublishedApi
internal fun IrFactory.buildClass(builder: IrClassBuilder): IrClass = with(builder) {
    createClass(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = origin,
        name = name,
        visibility = visibility,
        symbol = IrClassSymbolImpl(),
        kind = kind,
        modality = modality,
        isExternal = isExternal,
        isCompanion = isCompanion,
        isInner = isInner,
        isData = isData,
        isValue = isValue,
        isExpect = isExpect,
        isFun = isFun,
    )
}

inline fun IrFactory.buildClass(builder: IrClassBuilder.() -> Unit) =
    IrClassBuilder().run {
        builder()
        buildClass(this)
    }

@PublishedApi
internal fun IrFactory.buildField(builder: IrFieldBuilder): IrField = with(builder) {
    createField(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = origin,
        name = name,
        visibility = visibility,
        symbol = IrFieldSymbolImpl(),
        type = type,
        isFinal = isFinal,
        isStatic = isStatic,
        isExternal = isExternal,
    ).also {
        it.metadata = metadata
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

fun IrClass.addField(fieldName: Name, fieldType: IrType, fieldVisibility: DescriptorVisibility = DescriptorVisibilities.PRIVATE): IrField =
    addField {
        name = fieldName
        type = fieldType
        visibility = fieldVisibility
    }

fun IrClass.addField(
    fieldName: String,
    fieldType: IrType,
    fieldVisibility: DescriptorVisibility = DescriptorVisibilities.PRIVATE
): IrField =
    addField(Name.identifier(fieldName), fieldType, fieldVisibility)

@PublishedApi
internal fun IrFactory.buildProperty(builder: IrPropertyBuilder): IrProperty = with(builder) {
    createProperty(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = origin,
        name = name,
        visibility = visibility,
        modality = modality,
        symbol = IrPropertySymbolImpl(),
        isVar = isVar,
        isConst = isConst,
        isLateinit = isLateinit,
        isDelegated = isDelegated,
        isExternal = isExternal,
        containerSource = containerSource,
        isExpect = isExpect,
        isFakeOverride = isFakeOverride,
    )
}

inline fun IrFactory.buildProperty(builder: IrPropertyBuilder.() -> Unit) =
    IrPropertyBuilder().run {
        builder()
        buildProperty(this)
    }

inline fun IrClass.addProperty(builder: IrPropertyBuilder.() -> Unit): IrProperty =
    factory.buildProperty(builder).also { property ->
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

inline fun IrProperty.addSetter(builder: IrFunctionBuilder.() -> Unit = {}): IrSimpleFunction =
    IrFunctionBuilder().run {
        name = Name.special("<set-${this@addSetter.name}>")
        builder()
        factory.buildFunction(this).also { setter ->
            this@addSetter.setter = setter
            setter.correspondingPropertySymbol = this@addSetter.symbol
            setter.parent = this@addSetter.parent
        }
    }

fun IrProperty.addDefaultGetter(parentClass: IrClass, builtIns: IrBuiltIns) {
    val field = backingField!!
    addGetter {
        origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
        returnType = field.type
    }.apply {
        dispatchReceiverParameter = parentClass.thisReceiver!!.copyTo(this)
        body = factory.createBlockBody(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(
                IrReturnImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    builtIns.nothingType,
                    symbol,
                    IrGetFieldImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        field.symbol,
                        field.type,
                        IrGetValueImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            dispatchReceiverParameter!!.type,
                            dispatchReceiverParameter!!.symbol
                        )
                    )
                )
            )
        )
    }
}

inline fun IrProperty.addBackingField(builder: IrFieldBuilder.() -> Unit = {}): IrField =
    IrFieldBuilder().run {
        name = this@addBackingField.name
        origin = IrDeclarationOrigin.PROPERTY_BACKING_FIELD
        builder()
        factory.buildField(this).also { field ->
            this@addBackingField.backingField = field
            field.correspondingPropertySymbol = this@addBackingField.symbol
            field.parent = this@addBackingField.parent
        }
    }

@PublishedApi
internal fun IrFactory.buildFunction(builder: IrFunctionBuilder): IrSimpleFunction = with(builder) {
    createSimpleFunction(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = origin,
        name = name,
        visibility = visibility,
        isInline = isInline,
        isExpect = isExpect,
        returnType = returnType,
        modality = modality,
        symbol = IrSimpleFunctionSymbolImpl(),
        isTailrec = isTailrec,
        isSuspend = isSuspend,
        isOperator = isOperator,
        isInfix = isInfix,
        isExternal = isExternal,
        containerSource = containerSource,
        isFakeOverride = isFakeOverride,
    )
}

@PublishedApi
internal fun IrFactory.buildConstructor(builder: IrFunctionBuilder): IrConstructor = with(builder) {
    return createConstructor(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = origin,
        name = SpecialNames.INIT,
        visibility = visibility,
        isInline = isInline,
        isExpect = isExpect,
        returnType = returnType,
        symbol = IrConstructorSymbolImpl(),
        isPrimary = isPrimary,
        isExternal = isExternal,
        containerSource = containerSource,
    )
}

inline fun IrFactory.buildFun(builder: IrFunctionBuilder.() -> Unit): IrSimpleFunction =
    IrFunctionBuilder().run {
        builder()
        buildFunction(this)
    }

inline fun IrFactory.addFunction(klass: IrDeclarationContainer, builder: IrFunctionBuilder.() -> Unit): IrSimpleFunction =
    buildFun(builder).also { function ->
        klass.declarations.add(function)
        function.parent = klass
    }

inline fun IrClass.addFunction(builder: IrFunctionBuilder.() -> Unit): IrSimpleFunction =
    factory.addFunction(this, builder)

fun IrClass.addFunction(
    name: String,
    returnType: IrType,
    modality: Modality = Modality.FINAL,
    visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
    isStatic: Boolean = false,
    isSuspend: Boolean = false,
    isFakeOverride: Boolean = false,
    isInline: Boolean = false,
    origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET
): IrSimpleFunction =
    addFunction {
        this.startOffset = startOffset
        this.endOffset = endOffset
        this.name = Name.identifier(name)
        this.returnType = returnType
        this.modality = modality
        this.visibility = visibility
        this.isSuspend = isSuspend
        this.isFakeOverride = isFakeOverride
        this.isInline = isInline
        this.origin = origin
    }.apply {
        if (!isStatic) {
            val thisReceiver = parentAsClass.thisReceiver!!
            dispatchReceiverParameter = thisReceiver.copyTo(this, type = thisReceiver.type)
        }
    }

inline fun IrFactory.buildConstructor(builder: IrFunctionBuilder.() -> Unit): IrConstructor =
    IrFunctionBuilder().run {
        builder()
        buildConstructor(this)
    }

inline fun IrClass.addConstructor(builder: IrFunctionBuilder.() -> Unit = {}): IrConstructor =
    factory.buildConstructor {
        builder()
        returnType = defaultType
    }.also { constructor ->
        declarations.add(constructor)
        constructor.parent = this@addConstructor
    }

fun <D> buildReceiverParameter(
    parent: D,
    origin: IrDeclarationOrigin,
    type: IrType,
    startOffset: Int = parent.startOffset,
    endOffset: Int = parent.endOffset
): IrValueParameter
        where D : IrDeclaration, D : IrDeclarationParent =
    parent.factory.createValueParameter(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = origin,
        name = SpecialNames.THIS,
        type = type,
        isAssignable = false,
        symbol = IrValueParameterSymbolImpl(),
        index = UNDEFINED_PARAMETER_INDEX,
        varargElementType = null,
        isCrossinline = false,
        isNoinline = false,
        isHidden = false,
    ).also {
        it.parent = parent
    }

fun IrFactory.buildValueParameter(builder: IrValueParameterBuilder, parent: IrDeclarationParent): IrValueParameter =
    with(builder) {
        return createValueParameter(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            name = name,
            type = type,
            isAssignable = isAssignable,
            symbol = IrValueParameterSymbolImpl(),
            index = index,
            varargElementType = varargElementType,
            isCrossinline = isCrossInline,
            isNoinline = isNoinline,
            isHidden = isHidden
        ).also {
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
    addValueParameter(Name.identifier(name), type, origin)

fun IrFunction.addValueParameter(name: Name, type: IrType, origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED): IrValueParameter =
    addValueParameter {
        this.name = name
        this.type = type
        this.origin = origin
    }

@PublishedApi
internal fun IrFactory.buildTypeParameter(builder: IrTypeParameterBuilder, parent: IrDeclarationParent): IrTypeParameter =
    with(builder) {
        createTypeParameter(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            name = name,
            symbol = IrTypeParameterSymbolImpl(),
            variance = variance,
            index = index,
            isReified = isReified,
        ).also {
            it.superTypes = superTypes
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
    return IrVariableImpl(
        startOffset, endOffset, origin,
        IrVariableSymbolImpl(),
        name, type, isVar, isConst, isLateinit
    ).also {
        if (parent != null) {
            it.parent = parent
        }
    }
}
