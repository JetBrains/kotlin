/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util.builders

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.deepCopyWithoutPatchingParents
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
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

fun interface DefaultValuesPolicy {
    fun process(irExpressionBody: IrExpression, deepCopy: IrElementTransformerVoid): IrExpression?

    companion object {
        val remove = DefaultValuesPolicy { _, _  -> null }
        val leaveAsIs = DefaultValuesPolicy { it, _ -> it }
        val copy = DefaultValuesPolicy { it, copy -> it.transform(copy, null) }
        val replaceWithStub = DefaultValuesPolicy { _, _ -> IrExpressionBodyImpl(IrErrorExpression("stub")}
    }
}

fun IrSimpleFunction.toBuilder(
    asMemberOf: IrClass?,
    withName: Name? = null,
    typeSubstitutor: IrTypeSubstitutor = IrTypeSubstitutor.EMPTY,
) = IrSimpleFunctionBuilder(withName ?: this.name, this).also { builder ->
    if (asMemberOf != null) {
        builder.addDispatchReceiver(asMemberOf)
    }
    val typeParametersRemapping = mutableMapOf<IrTypeParameterSymbol, IrTypeArgument>()
    for (typeParameter in typeParameters) {
        if (!typeSubstitutor.hasSubstitutionFor(typeParameter.symbol)) {
            builder.typeParameters.add(IrTypeParameterBuilder(typeParameter))
            typeParametersRemapping[typeParameter.symbol] = builder.typeParameters.last().symbol.defaultType
        }
    }
    val fullTypeSubstitutor = IrChainedSubstitutor(
        IrTypeSubstitutor(typeParametersRemapping),
        typeSubstitutor
    )
    for (parameter in builder.typeParameters) {
        for (index in parameter.superTypes.indices) {
            parameter.superTypes[index] = fullTypeSubstitutor.substitute(parameter.superTypes[index])
        }
    }
    fun IrValueParameter.copy() : IrValueParameterBuilder {
        return IrValueParameterBuilder(fullTypeSubstitutor.substitute(type), this).also { builder ->
            defaultValue?.let { builder.defaultValue = it.deepCopyWithoutPatchingParents() }
        }
    }
    builder.returnType = fullTypeSubstitutor.substitute(returnType)
    for (i in 0 until contextReceiverParametersCount) {
        builder.contextParameters.add(valueParameters[i].copy())
    }
    extensionReceiverParameter?.let { builder.extensionReceiverParameter = it.copy() }
    for (i in contextReceiverParametersCount until valueParameters.size) {
        builder.valueParameters.add(valueParameters[i].copy())
    }

}