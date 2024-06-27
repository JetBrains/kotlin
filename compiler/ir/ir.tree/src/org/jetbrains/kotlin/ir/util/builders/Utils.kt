/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util.builders

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.DeepCopyTypeRemapper
import org.jetbrains.kotlin.ir.util.deepCopyWithoutPatchingParents
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name

fun IrFactory.buildSimpleFunction(
    builder: IrSimpleFunctionBuilder,
    parent: IrDeclarationParent,
): IrSimpleFunction {
    return builder
        .build(this)
        .also { it.parent = parent }
}

fun IrFactory.buildConstructor(
    builder: IrConstructorBuilder,
    parent: IrDeclarationParent,
): IrConstructor {
    return builder
        .build(this)
        .also { it.parent = parent }
}

fun IrDeclarationContainer.addSimpleFunction(
    builder: IrSimpleFunctionBuilder,
    factory: IrFactory,
): IrSimpleFunction {
    return factory.buildSimpleFunction(builder, this)
        .also { declarations.add(it) }
}

fun IrClass.addConstructor(
    builder: IrConstructorBuilder,
): IrConstructor {
    return factory.buildConstructor(builder, this)
        .also { declarations.add(it) }
}


inline fun IrFactory.buildSimpleFunction(
    name: Name,
    parent: IrDeclarationParent,
    builder: IrSimpleFunctionBuilder.() -> Unit
): IrSimpleFunction {
    return buildSimpleFunction(IrSimpleFunctionBuilder(name).apply(builder), parent)
}

inline fun IrFactory.buildConstructor(
    parent: IrDeclarationContainer,
    builder: IrConstructorBuilder.() -> Unit
): IrConstructor {
    return buildConstructor(IrConstructorBuilder().apply(builder), parent)
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
        this@addGetter.getter = it
        it.correspondingPropertySymbol = this@addGetter.symbol
        it.parent = this@addGetter.parent
    }
}

inline fun IrProperty.addSetter(builder: IrSimpleFunctionBuilder.() -> Unit): IrSimpleFunction {
    return factory.buildSimpleFunction(Name.special("<set-${this@addSetter.name}>"), parent) {
        (parent as? IrClass)?.let { addDispatchReceiver(it) }
    }.also {
        this@addSetter.setter = it
        it.correspondingPropertySymbol = this@addSetter.symbol
        it.parent = this@addSetter.parent
    }
}

private fun <T: IrFunction> T.fillBuilder(
    builder: IrFunctionBuilder<T>,
    typeSubstitutor: IrTypeSubstitutor,
    withBody: Boolean
) {
    val typeParametersRemapping = mutableMapOf<IrTypeParameterSymbol, IrTypeParameterSymbol>()
    val valueParametersRemapping = mutableMapOf<IrValueParameterSymbol, IrValueParameterSymbol>()
    for (typeParameter in typeParameters) {
        if (!typeSubstitutor.hasSubstitutionFor(typeParameter.symbol)) {
            builder.typeParameters.add(IrTypeParameterBuilder(typeParameter))
            typeParametersRemapping[typeParameter.symbol] = builder.typeParameters.last().symbol
        }
    }
    val fullTypeSubstitutor = IrChainedSubstitutor(
        IrTypeSubstitutor(typeParametersRemapping.mapValues { (_, it) -> it.defaultType }),
        typeSubstitutor
    )
    for (parameter in builder.typeParameters) {
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
    builder.returnType = fullTypeSubstitutor.substitute(returnType)
    dispatchReceiverParameter?.let { builder.dispatchReceiverParameter = it.copy() }
    for (i in 0 until contextReceiverParametersCount) {
        builder.contextParameters.add(valueParameters[i].copy())
    }
    extensionReceiverParameter?.let { builder.extensionReceiverParameter = it.copy() }
    for (i in contextReceiverParametersCount until valueParameters.size) {
        builder.valueParameters.add(valueParameters[i].copy())
    }
    if (withBody) {
        builder.body = body?.deepCopyWithoutPatchingParents(
            symbolRemapper = deepCopySymbolRemapper,
            createTypeRemapper = { DeepCopyTypeRemapper(it, typeSubstitutor) }
        )
    }

}

fun IrSimpleFunction.toBuilder(
    withName: Name? = null,
    withBody: Boolean = false,
    typeSubstitutor: IrTypeSubstitutor = IrTypeSubstitutor.EMPTY,
) = IrSimpleFunctionBuilder(withName ?: this.name, this).also { builder ->
    fillBuilder(builder, typeSubstitutor, withBody)
}


fun IrConstructor.toBuilder(
    withBody: Boolean = false,
    typeSubstitutor: IrTypeSubstitutor = IrTypeSubstitutor.EMPTY,
) = IrConstructorBuilder(this).also { builder ->
    fillBuilder(builder, typeSubstitutor, withBody)
}