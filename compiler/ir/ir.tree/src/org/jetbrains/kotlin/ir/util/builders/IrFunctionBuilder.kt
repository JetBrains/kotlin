/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util.builders

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeSubstitutor
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.IrTypeParameterRemapper
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.Variance

abstract class IrDeclarationBuilder<D: IrDeclaration> @PublishedApi internal constructor() {
    var startOffset: Int = UNDEFINED_OFFSET
    var endOffset: Int = UNDEFINED_OFFSET
    var origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED

    lateinit var name: Name

    open fun updateFrom(from: D) {
        startOffset = from.startOffset
        endOffset = from.endOffset
        origin = from.origin
    }
}

abstract class IrDeclarationWithVisibilityBuilder<D: IrDeclarationWithVisibility> @PublishedApi internal constructor() : IrDeclarationBuilder<D>() {
    var visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC

    override fun updateFrom(from: D) {
        super.updateFrom(from)
        visibility = from.visibility
    }
}

abstract class IrFunctionBuilder<F: IrFunction> @PublishedApi internal constructor() : IrDeclarationWithVisibilityBuilder<F>() {
    var isInline: Boolean = false
    var isExternal: Boolean = false
    var isExpect: Boolean = false
    var containerSource: DeserializedContainerSource? = null

    lateinit var returnType: IrType

    class IrTypeParameterBuilder @PublishedApi internal constructor(): IrDeclarationBuilder<IrTypeParameter>() {
        var variance: Variance = Variance.INVARIANT
        var isReified: Boolean = false
        val superTypes: MutableList<IrType> = mutableListOf()
        val symbol: IrTypeParameterSymbol = IrTypeParameterSymbolImpl()

        override fun updateFrom(from: IrTypeParameter) {
            variance = from.variance
            isReified = from.isReified
            superTypes.clear()
            superTypes.addAll(from.superTypes.toMutableList())
        }

        @PublishedApi
        internal fun build(factory: IrFactory, index: Int, typeRemapper: TypeRemapper) : IrTypeParameter = factory.createTypeParameter(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            name = name,
            symbol = symbol,
            variance = variance,
            index = index,
            isReified = isReified
        ).also {
            it.superTypes = superTypes.map { typeRemapper.remapType(it) }
        }
    }

    class IrValueParameterBuilder @PublishedApi internal constructor() : IrDeclarationBuilder<IrValueParameter>() {
        lateinit var type: IrType

        var varargElementType: IrType? = null
        var isCrossInline: Boolean = false
        var isNoinline: Boolean = false
        var isHidden: Boolean = false
        var isAssignable: Boolean = false
        var originalDefaultValue: IrExpressionBody? = null
        val symbol: IrValueParameterSymbol = IrValueParameterSymbolImpl()

        override fun updateFrom(from: IrValueParameter) {
            super.updateFrom(from)
            type = from.type
            varargElementType = from.varargElementType
            isCrossInline = from.isCrossinline
            isNoinline = from.isNoinline
            isHidden = from.isHidden
            isAssignable = from.isAssignable
            originalDefaultValue = from.defaultValue
        }

        @PublishedApi
        internal fun build(factory: IrFactory, index: Int, irFunction: IrFunction, typeRemapper: TypeRemapper, symbolRemapper: DeepCopySymbolRemapper): IrValueParameter =
            factory.createValueParameter(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = name,
                type = typeRemapper.remapType(type),
                isAssignable = isAssignable,
                symbol = symbol,
                index = index,
                varargElementType = varargElementType?.let { typeRemapper.remapType(it) },
                isCrossinline = isCrossInline,
                isNoinline = isNoinline,
                isHidden = isHidden,
            ).also {
                it.parent = irFunction
                originalDefaultValue?.let { originalDefaultValue ->
                    it.defaultValue = originalDefaultValue.deepCopyWithSymbols(
                        initialParent = irFunction,
                        symbolRemapper = symbolRemapper,
                    )
                }
            }
    }

    var typeParameters = mutableListOf<IrTypeParameterBuilder>()
    var dispatchReceiverParameter: IrValueParameterBuilder? = null
    var extensionReceiverParameter: IrValueParameterBuilder? = null
    var contextParameters = mutableListOf<IrValueParameterBuilder>()
    var valueParameters = mutableListOf<IrValueParameterBuilder>()
    var typeParametersRemapping = mutableMapOf<IrTypeParameterSymbol, IrTypeParameterSymbol>()
    var valueParameterRemapping = mutableMapOf<IrValueParameterSymbol, IrValueParameterSymbol>()
    var originalBody: IrBody? = null

    override fun updateFrom(from: F) {
        super.updateFrom(from)
        containerSource = from.containerSource
        isInline = from.isInline
        isExternal = from.isExternal
        isExpect = from.isExpect
    }

    protected abstract fun create(factory: IrFactory): F

    fun build(factory: IrFactory): F = create(factory).also {
        val symbolRemapper = object : DeepCopySymbolRemapper() {
            override fun getReferencedTypeParameter(symbol: IrTypeParameterSymbol): IrClassifierSymbol {
                typeParametersRemapping[symbol]?.let { return it }
                return super.getReferencedTypeParameter(symbol)
            }
            override fun getReferencedValueParameter(symbol: IrValueParameterSymbol): IrValueSymbol {
                valueParameterRemapping[symbol]?.let { return it }
                return super.getReferencedValueParameter(symbol)
            }
        }
        val typeRemapper = IrTypeParameterRemapper(typeParametersRemapping)

        it.typeParameters = typeParameters.mapIndexed { index, builder ->
            builder.build(factory, index, typeRemapper).apply {
                parent = it
            }
        }.toMutableList()
        it.dispatchReceiverParameter = dispatchReceiverParameter?.build(factory, -1, it, typeRemapper, symbolRemapper)
        it.extensionReceiverParameter = extensionReceiverParameter?.build(factory, -1, it, typeRemapper, symbolRemapper)
        it.contextReceiverParametersCount = contextParameters.size
        it.valueParameters = (contextParameters + valueParameters).mapIndexed { index, builder ->
            builder.build(factory, index, it, typeRemapper, symbolRemapper)
        }.toMutableList()
        it.contextReceiverParametersCount = contextParameters.size
        originalBody?.let { originalBody ->
            it.body = originalBody.deepCopyWithSymbols(it, symbolRemapper)
        }
    }

    val IrTypeParameterBuilder.defaultType
        get() = IrSimpleTypeImpl(
            symbol,
            SimpleTypeNullability.NOT_SPECIFIED,
            arguments = emptyList(),
            annotations = emptyList()
        )

    inline fun addTypeParameter(builder: IrTypeParameterBuilder.() -> Unit) {
        typeParameters.add(IrTypeParameterBuilder().apply { builder() })
    }

    inline fun addDispatchReceiver(builder: IrValueParameterBuilder.() -> Unit) {
        dispatchReceiverParameter = IrValueParameterBuilder().apply { builder() }
    }

    inline fun addExtensionReceiver(builder: IrValueParameterBuilder.() -> Unit) {
        extensionReceiverParameter = IrValueParameterBuilder().apply { builder() }
    }

    inline fun addContextParameter(builder: IrValueParameterBuilder.() -> Unit) {
        contextParameters.add(IrValueParameterBuilder().apply { builder() })
    }

    inline fun addValueParameter(builder: IrValueParameterBuilder.() -> Unit) {
        valueParameters.add(IrValueParameterBuilder().apply { builder() })
    }

    private fun IrValueParameterBuilder.copyFrom(from: IrValueParameter, typeSubstitutor: IrTypeSubstitutor?) {
        updateFrom(from)
        name = from.name
        valueParameterRemapping[from.symbol] = symbol
        if (typeSubstitutor != null) {
            type = typeSubstitutor.substitute(type)
            varargElementType = varargElementType?.let { typeSubstitutor.substitute(it) }
        }
    }

    fun copyDispatchReceiver(from: IrValueParameter, typeSubstitutor: IrTypeSubstitutor? = null) = addDispatchReceiver { copyFrom(from, typeSubstitutor) }
    fun copyExtensionReceiver(from: IrValueParameter, typeSubstitutor: IrTypeSubstitutor? = null) = addExtensionReceiver { copyFrom(from, typeSubstitutor) }
    fun copyContextParameter(from: IrValueParameter, typeSubstitutor: IrTypeSubstitutor? = null) = addContextParameter { copyFrom(from, typeSubstitutor) }
    fun copyValueParameter(from: IrValueParameter, typeSubstitutor: IrTypeSubstitutor? = null) = addValueParameter { copyFrom(from, typeSubstitutor) }
    fun copyTypeParameter(from: IrTypeParameter, typeSubstitutor: IrTypeSubstitutor? = null) = addTypeParameter {
        updateFrom(from)
        name = from.name
        typeParametersRemapping[from.symbol] = symbol
        if (typeSubstitutor != null) {
            for (i in superTypes.indices) {
                superTypes[i] = typeSubstitutor.substitute(superTypes[i])
            }
        }
    }
    fun copyValueParametersFrom(from: IrFunction, typeSubstitutor: IrTypeSubstitutor? = null) {
        from.dispatchReceiverParameter?.let { copyDispatchReceiver(it, typeSubstitutor) }
        for (i in 0 until from.contextReceiverParametersCount) {
            copyContextParameter(from.valueParameters[i], typeSubstitutor)
        }
        from.extensionReceiverParameter?.let { copyExtensionReceiver(it, typeSubstitutor) }
        for (i in from.contextReceiverParametersCount until from.valueParameters.size) {
            copyValueParameter(from.valueParameters[i], typeSubstitutor)
        }
    }
    fun copyValueParametersFromWithShape(
        from: IrFunction,
        withDispatchReceiver: Boolean,
        withContextParametersCount: Int,
        withExtensionReceiver: Boolean,
        typeSubstitutor: IrTypeSubstitutor? = null
    ) {
        val parameters = buildList {
            from.dispatchReceiverParameter?.let { add(it) }
            for (i in 0 until from.contextReceiverParametersCount) {
                add(from.valueParameters[i])
            }
            from.extensionReceiverParameter?.let { add(it) }
            for (i in from.contextReceiverParametersCount until from.valueParameters.size) {
                add(from.valueParameters[i])
            }
        }
        var parameterId: Int = 0
        if (withDispatchReceiver) {
            copyDispatchReceiver(parameters[parameterId++], typeSubstitutor)
        }
        for (i in 0 until withContextParametersCount) {
            copyContextParameter(parameters[parameterId++], typeSubstitutor)
        }
        if (withExtensionReceiver) {
            copyExtensionReceiver(parameters[parameterId++], typeSubstitutor)
        }
        while (parameterId < parameters.size) {
            copyValueParameter(parameters[parameterId++], typeSubstitutor)
        }
    }
    fun copyTypeParametersFrom(from: IrFunction) {
        for (typeParameter in from.typeParameters) {
            copyTypeParameter(typeParameter)
        }
    }
    fun copyTypeAndValueParametersFrom(from: IrFunction) {
        copyTypeParametersFrom(from)
        copyValueParametersFrom(from)
    }
}

class IrSimpleFunctionBuilder @PublishedApi internal constructor(): IrFunctionBuilder<IrSimpleFunction>() {
    var modality: Modality = Modality.FINAL
    var isTailrec: Boolean = false
    var isSuspend: Boolean = false
    var isOperator: Boolean = false
    var isInfix: Boolean = false
    var isFakeOverride: Boolean = false

    override fun updateFrom(from: IrSimpleFunction) {
        super.updateFrom(from)
        modality = from.modality
        isTailrec = from.isTailrec
        isSuspend = from.isSuspend
        isOperator = from.isOperator
        isInfix = from.isInfix
        isFakeOverride = from.isFakeOverride
    }

    override fun create(factory: IrFactory) = factory.createSimpleFunction(
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
        isFakeOverride = isFakeOverride
    )
}

class IrConstructorBuilder @PublishedApi internal constructor(): IrFunctionBuilder<IrConstructor>() {
    var isPrimary = false
    override fun updateFrom(from: IrConstructor) {
        super.updateFrom(from)
        isPrimary = from.isPrimary
    }

    override fun create(factory: IrFactory): IrConstructor = factory.createConstructor(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = origin,
        name = name,
        visibility = visibility,
        isInline = isInline,
        isExpect = isExpect,
        returnType = returnType,
        symbol = IrConstructorSymbolImpl(),
        isPrimary = isPrimary,
        isExternal = isExpect,
        containerSource = containerSource,
    )
}

inline fun IrFactory.buildSimpleFunction(parent: IrDeclarationParent? = null, builder: IrSimpleFunctionBuilder.() -> Unit): IrSimpleFunction =
    IrSimpleFunctionBuilder().apply { builder() }.build(this).also {
        if (parent != null) {
            it.parent = parent
        }
    }


inline fun IrFactory.buildConstructor(parent: IrDeclarationContainer? = null, builder: IrConstructorBuilder.() -> Unit): IrConstructor =
    IrConstructorBuilder().apply {
        name = SpecialNames.INIT
        builder()
    }.build(this).also {
        if (parent != null) {
            it.parent = parent
        }
    }

inline fun IrDeclarationContainer.addSimpleFunction(factory: IrFactory, builder: IrSimpleFunctionBuilder.() -> Unit): IrSimpleFunction =
    factory.buildSimpleFunction(this, builder).also {
        declarations.add(it)
    }

inline fun IrClass.addSimpleFunction(builder: IrSimpleFunctionBuilder.() -> Unit): IrSimpleFunction =
    addSimpleFunction(factory, builder)

inline fun IrClass.addConstructor(builder: IrConstructorBuilder.() -> Unit) =
    factory.buildConstructor(this) {
        returnType = defaultType
        builder()
    }

inline fun IrProperty.addGetter(builder: IrSimpleFunctionBuilder.() -> Unit) =
    factory.buildSimpleFunction {
        name = Name.special("<get-${this@addGetter.name}>")
        builder()
    }.also {
        this@addGetter.getter = it
        it.correspondingPropertySymbol = this@addGetter.symbol
        it.parent = this@addGetter.parent
    }

inline fun IrProperty.addSetter(builder: IrSimpleFunctionBuilder.() -> Unit) =
    factory.buildSimpleFunction {
        name = Name.special("<set-${this@addSetter.name}>")
        builder()
    }.also {
        this@addSetter.setter = it
        it.correspondingPropertySymbol = this@addSetter.symbol
        it.parent = this@addSetter.parent
    }
