/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util.builders

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.DeepCopyTypeRemapper
import org.jetbrains.kotlin.ir.util.deepCopyWithoutPatchingParents
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.Name


fun IrDeclarationContainer.addSimpleFunction(
    builder: IrSimpleFunctionBuilder,
    factory: IrFactory,
): IrSimpleFunction {
    return builder.build(factory, this)
        .also { declarations.add(it) }
}

fun IrClass.addConstructor(
    builder: IrConstructorBuilder,
): IrConstructor {
    return builder.build(factory, this)
        .also { declarations.add(it) }
}

inline fun IrFactory.buildSimpleFunction(
    name: Name,
    parent: IrDeclarationParent,
    builder: IrSimpleFunctionBuilder.() -> Unit
): IrSimpleFunction {
    return IrSimpleFunctionBuilder(name).apply(builder).build(this, parent)
}

inline fun IrFactory.buildConstructor(
    parent: IrDeclarationContainer,
    builder: IrConstructorBuilder.() -> Unit
): IrConstructor {
    return IrConstructorBuilder().apply(builder).build(this, parent)
}

inline fun IrDeclarationContainer.addSimpleStaticFunction(
    name: Name,
    factory: IrFactory,
    builder: IrSimpleFunctionBuilder.() -> Unit
): IrSimpleFunction {
    return addSimpleFunction(IrSimpleFunctionBuilder(name).apply(builder), factory)
}

inline fun IrClass.addSimpleMemberFunction(
    name: Name,
    builder: IrSimpleFunctionBuilder.() -> Unit
): IrSimpleFunction {
    return addSimpleFunction(IrSimpleFunctionBuilder(name).apply {
        addDispatchReceiver(this@addSimpleMemberFunction)
        builder()
    }, factory)
}

inline fun IrClass.addConstructor(builder: IrConstructorBuilder.() -> Unit): IrConstructor {
    return addConstructor(IrConstructorBuilder().apply {
        returnType = defaultType
        builder()
    })
}

inline fun IrProperty.addGetter(builder: IrSimpleFunctionBuilder.() -> Unit): IrSimpleFunction {
    return factory.buildSimpleFunction(Name.special("<get-${this@addGetter.name}>"), parent) {
        (parent as? IrClass)?.let { addDispatchReceiver(it) }
        builder()
    }.also {
        it.correspondingPropertySymbol = this@addGetter.symbol
        this@addGetter.getter = it
    }
}

inline fun IrProperty.addSetter(builder: IrSimpleFunctionBuilder.() -> Unit): IrSimpleFunction {
    return factory.buildSimpleFunction(Name.special("<set-${this@addSetter.name}>"), parent) {
        (parent as? IrClass)?.let { addDispatchReceiver(it) }
    }.also {
        it.correspondingPropertySymbol = this@addSetter.symbol
        this@addSetter.setter = it
    }
}

fun <T: IrFunction> IrFunctionBuilder<T>.fillParametersFrom(
    function: T,
    typeSubstitutor: IrTypeSubstitutor? = null,
    withBody: Boolean = false
) {
    val typeParametersRemapping = mutableMapOf<IrTypeParameterSymbol, IrTypeParameterSymbol>()
    val valueParametersRemapping = mutableMapOf<IrValueParameterSymbol, IrValueParameterSymbol>()
    for (typeParameter in function.typeParameters) {
        if (typeSubstitutor?.hasSubstitutionFor(typeParameter.symbol) != true) {
            typeParameters.add(IrTypeParameterBuilder(typeParameter))
            typeParametersRemapping[typeParameter.symbol] = typeParameters.last().symbol
        }
    }
    val partialSubstitutor = IrTypeSubstitutor(typeParametersRemapping.mapValues { (_, it) -> it.defaultType }, allowEmptySubstitution = true)
    val fullTypeSubstitutor = if (typeSubstitutor == null) { partialSubstitutor } else {
        IrChainedSubstitutor(
            partialSubstitutor,
            typeSubstitutor
        )
    }
    for (parameter in typeParameters) {
        for (index in parameter.superTypes.indices) {
            parameter.superTypes[index] = fullTypeSubstitutor.substitute(parameter.superTypes[index])
        }
    }
    val deepCopySymbolRemapper = object : DeepCopySymbolRemapper() {
        override fun getReferencedTypeParameter(symbol: IrTypeParameterSymbol): IrClassifierSymbol {
            typeParametersRemapping[symbol]?.let { return it }
            return super.getReferencedTypeParameter(symbol)
        }

        override fun getReferencedValueParameter(symbol: IrValueParameterSymbol): IrValueSymbol {
            valueParametersRemapping[symbol]?.let { return it }
            return super.getReferencedValueParameter(symbol)
        }
    }
    fun IrValueParameter.copy() : IrValueParameterBuilder {
        return IrValueParameterBuilder(fullTypeSubstitutor.substitute(type), this).also { builder ->
            valueParametersRemapping[this.symbol] = builder.symbol
            defaultValue?.let {
                builder.defaultValue = it.deepCopyWithoutPatchingParents(
                    symbolRemapper = deepCopySymbolRemapper,
                    createTypeRemapper = { DeepCopyTypeRemapper(it, typeSubstitutor) }
                )
            }
        }
    }
    returnType = fullTypeSubstitutor.substitute(function.returnType)
    function.dispatchReceiverParameter?.let { dispatchReceiverParameter = it.copy() }
    for (i in 0 until function.contextReceiverParametersCount) {
        contextParameters.add(function.valueParameters[i].copy())
    }
    function.extensionReceiverParameter?.let { extensionReceiverParameter = it.copy() }
    for (i in function.contextReceiverParametersCount until function.valueParameters.size) {
        valueParameters.add(function.valueParameters[i].copy())
    }
    if (withBody) {
        body = function.body?.deepCopyWithoutPatchingParents(
            symbolRemapper = deepCopySymbolRemapper,
            createTypeRemapper = { DeepCopyTypeRemapper(it, typeSubstitutor) }
        )
    }
}

fun IrSimpleFunction.toBuilder(
    withName: Name? = null,
    withParameters: Boolean = true,
    withBody: Boolean = false,
    typeSubstitutor: IrTypeSubstitutor? = null,
): IrSimpleFunctionBuilder = IrSimpleFunctionBuilder(withName ?: this.name, this).also { builder ->
    if (withParameters) {
        builder.fillParametersFrom(this, typeSubstitutor, withBody)
    }
}

inline fun IrSimpleFunction.toBuilder(
    withName: Name? = null,
    withParameters: Boolean = true,
    withBody: Boolean = false,
    typeSubstitutor: IrTypeSubstitutor? = null,
    builder: IrSimpleFunctionBuilder.() -> Unit
): IrSimpleFunctionBuilder = toBuilder(withName, withParameters, withBody, typeSubstitutor).apply(builder)


fun IrConstructor.toBuilder(
    withBody: Boolean = false,
    withParameters: Boolean = true,
    typeSubstitutor: IrTypeSubstitutor? = null,
): IrConstructorBuilder = IrConstructorBuilder(this).also { builder ->
    if (withParameters) {
        builder.fillParametersFrom(this, typeSubstitutor, withBody)
    }
}

inline fun IrConstructor.toBuilder(
    withBody: Boolean = false,
    withParameters: Boolean = true,
    typeSubstitutor: IrTypeSubstitutor? = null,
    builder: IrConstructorBuilder.() -> Unit
): IrConstructorBuilder = toBuilder(withBody, withParameters, typeSubstitutor).apply(builder)