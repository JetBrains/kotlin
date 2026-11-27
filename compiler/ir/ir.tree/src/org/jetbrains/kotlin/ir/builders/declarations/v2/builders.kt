/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("IrDeclarationBuilders")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ir.builders.declarations.v2


import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.typeWithParameters
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedMapIndexed

@PublishedApi
internal fun IrClass.buildMemberFunction(builder: IrSimpleFunctionBuilder): IrSimpleFunction {
    builder.addDefaultDispatchReceiver(this)
    return addMember(builder) { factory.build(it) }
}

@PublishedApi
internal fun IrClass.buildStaticFunction(builder: IrSimpleFunctionBuilder): IrSimpleFunction {
    builder.checkNoDispatchReceiver()
    return addMember(builder) { factory.build(it) }
}

@PublishedApi
internal fun IrClass.buildMemberConstructor(builder: IrConstructorBuilder): IrConstructor {
    if (builder.returnType == null) builder.returnType = defaultType
    return addMember(builder) { factory.build(it) }
}

@PublishedApi
internal fun IrClass.buildMemberField(builder: IrFieldBuilder): IrField =
    addMember(builder) { factory.build(it) }

@PublishedApi
internal fun IrClass.buildMemberProperty(builder: IrPropertyBuilder): IrProperty {
    builder.getter?.addDefaultDispatchReceiver(this)
    builder.setter?.addDefaultDispatchReceiver(this)
    return addMember(builder) { factory.build(it) }
}

@PublishedApi
internal fun IrClass.buildStaticProperty(builder: IrPropertyBuilder): IrProperty {
    builder.getter?.checkNoDispatchReceiver()
    builder.setter?.checkNoDispatchReceiver()
    return addMember(builder) { factory.build(it) }
}

@PublishedApi
internal fun IrClass.buildMemberAnonymousInitializer(builder: IrAnonymousInitializerBuilder): IrAnonymousInitializer =
    addMember(builder) { factory.build(it) }

@PublishedApi
internal fun IrBuilderWithScope.buildLocalVariable(builder: IrVariableBuilder): IrVariable {
    checkNoExplicitParent(builder)
    builder.parent = scope.getLocalDeclarationParent()
    return context.irFactory.build(builder)
}

@PublishedApi
internal fun IrProperty.buildBackingField(builder: IrFieldBuilder): IrField {
    check(backingField == null) { "The backing field of $name is already set" }
    if (!builder.isNameInitialized) builder.name = name
    return factory.build(asPropertyChild(builder, this)).also {
        backingField = it
        it.correspondingPropertySymbol = symbol
    }
}

@PublishedApi
internal fun IrProperty.buildGetter(builder: IrSimpleFunctionBuilder): IrSimpleFunction {
    check(getter == null) { "The getter of $name is already set" }
    return buildAccessor(builder, "get").also { getter = it }
}

@PublishedApi
internal fun IrProperty.buildSetter(builder: IrSimpleFunctionBuilder): IrSimpleFunction {
    check(setter == null) { "The setter of $name is already set" }
    return buildAccessor(builder, "set").also { setter = it }
}


private fun IrProperty.buildAccessor(accessor: IrSimpleFunctionBuilder, prefix: String): IrSimpleFunction {
    if (!accessor.isNameInitialized) accessor.name = Name.special("<$prefix-$name>")
    (parent as? IrClass)?.let { accessor.addDefaultDispatchReceiver(it) }
    return factory.build(asPropertyChild(accessor, this)).also { it.correspondingPropertySymbol = symbol }
}

private fun <B : IrDeclarationBuilder> asPropertyChild(child: B, property: IrProperty): B {
    checkNoExplicitParent(child)
    child.parent = property.parent
    return child
}

private fun IrFunctionBuilder.addDefaultDispatchReceiver(klass: IrClass) {
    if (!hasDispatchReceiver) {
        dispatchReceiver { copyFrom(klass.thisReceiver!!) }
    }
}

private fun IrFunctionBuilder.checkNoDispatchReceiver() {
    check(!hasDispatchReceiver) { "Static function $nameForMessages has a dispatch receiver" }
}

private inline fun <B : IrDeclarationBuilder, D : IrDeclaration> IrClass.addMember(builder: B, build: (B) -> D): D {
    checkNoExplicitParent(builder)
    builder.parent = this
    return build(builder).also { declarations += it }
}

private fun checkNoExplicitParent(builder: IrDeclarationBuilder) {
    check(builder.parent == null) { "The parent of ${builder.nameForMessages} is set by its owner, it must not be set in the builder" }
}

/*
 * Hooks of the generated `IrFactory.build(builder)` (next to the builders): the parts that depend on the owner and the order of initialization.
 */

internal fun IrFactory.buildChildren(builder: IrClassBuilder, klass: IrClass) {
    // Before the receiver: its type is the class type with the class type parameters.
    klass.typeParameters = buildTypeParameters(builder.typeParameters, klass)
    builder.thisReceiver?.let { receiver ->
        if (receiver.startOffset == UNDEFINED_OFFSET) receiver.startOffset = klass.startOffset
        if (receiver.endOffset == UNDEFINED_OFFSET) receiver.endOffset = klass.endOffset
        if (receiver.origin == IrDeclarationOrigin.DEFINED) receiver.origin = IrDeclarationOrigin.INSTANCE_RECEIVER
        if (!receiver.isTypeInitialized) receiver.type = klass.symbol.typeWithParameters(klass.typeParameters)
        klass.thisReceiver = buildChild(receiver, klass)
    }
    for (declaration in builder.declarations) {
        declaration.parent = klass
    }
    klass.declarations.addAll(builder.declarations)
}

internal fun IrFactory.buildChildren(builder: IrFunctionBuilder, function: IrFunction) {
    function.typeParameters = buildTypeParameters(builder.typeParameters, function)
    // Parameters are ordered by kind: dispatch receiver, context parameters, extension receiver, regular parameters.
    // The builder is single-shot, so its list is sorted in place (the sort is stable).
    builder.parameters.sortBy { it.kind.ordinal }
    function.parameters = builder.parameters.memoryOptimizedMap { buildChild(it, function) }
}

internal fun IrFactory.buildChildren(builder: IrPropertyBuilder, property: IrProperty) {
    // The children are siblings of the property in the IR tree: they share its parent, if it's known.
    property.backingField = builder.backingField?.let { field ->
        if (!field.isNameInitialized) field.name = builder.name
        build(asPropertyChild(field, builder)).also { it.correspondingPropertySymbol = property.symbol }
    }
    property.getter = builder.getter?.let { getter ->
        if (!getter.isNameInitialized) getter.name = Name.special("<get-${builder.name}>")
        build(asPropertyChild(getter, builder)).also { it.correspondingPropertySymbol = property.symbol }
    }
    property.setter = builder.setter?.let { setter ->
        if (!setter.isNameInitialized) setter.name = Name.special("<set-${builder.name}>")
        build(asPropertyChild(setter, builder)).also { it.correspondingPropertySymbol = property.symbol }
    }
}

internal fun applyDefaults(builder: IrVariableBuilder) {
    if (!builder.isTypeInitialized) {
        builder.type = checkNotNull(builder.initializer) {
            "The type of variable ${builder.nameForMessages} is neither set nor defined by an initializer"
        }.type
    }
}

private fun <B : IrDeclarationBuilder> asPropertyChild(child: B, property: IrPropertyBuilder): B {
    checkNoExplicitParent(child)
    child.parent = property.parent
    return child
}

private fun IrFactory.buildChild(builder: IrParameterBuilder, owner: IrDeclarationParent): IrValueParameter {
    checkNoExplicitParent(builder)
    return build(builder).also { it.parent = owner }
}

private fun IrFactory.buildTypeParameters(
    builders: List<IrTypeParameterBuilder>,
    owner: IrTypeParametersContainer,
): List<IrTypeParameter> =
    builders.memoryOptimizedMapIndexed { index, builder ->
        checkNoExplicitParent(builder)
        build(builder, index).also { it.parent = owner }
    }
