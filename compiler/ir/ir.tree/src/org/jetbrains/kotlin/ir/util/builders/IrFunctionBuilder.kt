/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util.builders

import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class IrFunctionBuilder<F: IrFunction> : IrDeclarationWithVisibilityBuilder<F> {
    var isInline: Boolean
    var isExternal: Boolean
    var isExpect: Boolean
    var containerSource: DeserializedContainerSource?
    lateinit var returnType: IrType

    @PublishedApi internal constructor(name: Name) : super(name) {
        isInline = false
        isExternal = false
        isExpect = false
        containerSource = null
    }
    @PublishedApi internal constructor(name: Name, from: F) : super(name, from) {
        isInline = from.isInline
        isExternal = from.isExternal
        isExpect = from.isExpect
        containerSource = from.containerSource
    }

    var typeParameters = mutableListOf<IrTypeParameterBuilder>()
    var dispatchReceiverParameter: IrValueParameterBuilder? = null
    var extensionReceiverParameter: IrValueParameterBuilder? = null
    var contextParameters = mutableListOf<IrValueParameterBuilder>()
    var valueParameters = mutableListOf<IrValueParameterBuilder>()
    var typeParametersRemapping = mutableMapOf<IrTypeParameterSymbol, IrTypeParameterSymbol>()
    var valueParameterRemapping = mutableMapOf<IrValueParameterSymbol, IrValueParameterSymbol>()

    protected abstract fun create(factory: IrFactory): F

    fun build(factory: IrFactory): F = create(factory).also {
        it.typeParameters = typeParameters.mapIndexed { index, builder ->
            builder.build(factory, index, it).apply {
                parent = it
            }
        }.toMutableList()
        it.dispatchReceiverParameter = dispatchReceiverParameter?.build(factory, -1, it)
        it.extensionReceiverParameter = extensionReceiverParameter?.build(factory, -1, it)
        it.contextReceiverParametersCount = contextParameters.size
        it.valueParameters = (contextParameters + valueParameters).mapIndexed { index, builder ->
            builder.build(factory, index, it)
        }.toMutableList()
        it.contextReceiverParametersCount = contextParameters.size
    }

    fun reshapeParameters(
        withDispatchReceiver: Boolean,
        withContextParametersCount: Int,
        withExtensionReceiver: Boolean,
    ) {
        val parameters = buildList {
            addIfNotNull(dispatchReceiverParameter)
            addAll(contextParameters)
            addIfNotNull(extensionReceiverParameter)
            addAll(valueParameters)
        }
        var parameterId = 0
        if (withDispatchReceiver) {
            dispatchReceiverParameter = parameters[parameterId++]
        } else {
            dispatchReceiverParameter = null
        }
        contextParameters.clear()
        for (i in 0 until withContextParametersCount) {
            contextParameters.add(parameters[parameterId++])
        }
        if (withExtensionReceiver) {
            extensionReceiverParameter = parameters[parameterId++]
        } else {
            extensionReceiverParameter = null
        }
        valueParameters.clear()
        while (parameterId < parameters.size) {
            valueParameters.add(parameters[parameterId++])
        }
    }

    /*val IrTypeParameterBuilder.defaultType
        get() = IrSimpleTypeImpl(
            symbol,
            SimpleTypeNullability.NOT_SPECIFIED,
            arguments = emptyList(),
            annotations = emptyList()
        )

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

    fun copyTypeParametersFrom(from: IrFunction) {
        for (typeParameter in from.typeParameters) {
            copyTypeParameter(typeParameter)
        }
    }
    fun copyTypeAndValueParametersFrom(from: IrFunction) {
        copyTypeParametersFrom(from)
        copyValueParametersFrom(from)
    }*/
}

class IrSimpleFunctionBuilder : IrFunctionBuilder<IrSimpleFunction> {
    var modality: Modality
    var isTailrec: Boolean
    var isSuspend: Boolean
    var isOperator: Boolean
    var isInfix: Boolean
    var isFakeOverride: Boolean

    @PublishedApi internal constructor(name: Name) : super(name) {
        modality = Modality.FINAL
        isTailrec = false
        isSuspend = false
        isOperator = false
        isInline = false
        isInfix = false
        isFakeOverride = false
    }
    @PublishedApi internal constructor(name: Name, from: IrSimpleFunction) : super(name, from) {
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

class IrConstructorBuilder : IrFunctionBuilder<IrConstructor> {
    var isPrimary: Boolean
    @PublishedApi internal constructor(): super(SpecialNames.INIT) {
        isPrimary = false
    }
    @PublishedApi internal constructor(from: IrConstructor): super(SpecialNames.INIT, from) {
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

inline fun <F : IrFunction> IrFunctionBuilder<F>.addValueParameter(name: Name, type: IrType, builder: IrValueParameterBuilder.() -> Unit) {
    valueParameters.add(IrValueParameterBuilder(name, type).apply { builder() })
}

inline fun <F : IrFunction> IrFunctionBuilder<F>.addContextParameter(name: Name, type: IrType, builder: IrValueParameterBuilder.() -> Unit) {
    contextParameters.add(IrValueParameterBuilder(name, type).apply { builder() })
}

inline fun <F : IrFunction> IrFunctionBuilder<F>.addExtensionReceiver(type: IrType, builder: IrValueParameterBuilder.() -> Unit) {
    extensionReceiverParameter = IrValueParameterBuilder(SpecialNames.RECEIVER, type).apply { builder() }
}

inline fun <F : IrFunction> IrFunctionBuilder<F>.addDispatchReceiver(type: IrType, builder: IrValueParameterBuilder.() -> Unit) {
    dispatchReceiverParameter = IrValueParameterBuilder(SpecialNames.THIS, type).apply { builder() }
}

inline fun <F : IrFunction> IrFunctionBuilder<F>.addTypeParameter(from: IrTypeParameter, builder: IrTypeParameterBuilder.() -> Unit) {
    typeParameters.add(IrTypeParameterBuilder(from).apply { builder() })
}

fun IrSimpleFunctionBuilder.addDispatchReceiver(clazz: IrClass) = addDispatchReceiver(clazz.defaultType) {
    origin = clazz.origin
}
