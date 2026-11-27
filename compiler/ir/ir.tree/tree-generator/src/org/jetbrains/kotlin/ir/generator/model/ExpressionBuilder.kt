/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import org.jetbrains.kotlin.generators.tree.ClassRef
import org.jetbrains.kotlin.generators.tree.NamedTypeParameterRef
import org.jetbrains.kotlin.generators.tree.TypeKind

/**
 * The properties of a builder are the fields of [element] that aren't fields of the elements of the [parent] builders,
 * see [org.jetbrains.kotlin.ir.generator.ExpressionBuilderConfigurator].
 */
class ExpressionBuilder(val element: Element, val parent: ExpressionBuilder?) {
    val typeName: String = element.typeName + "Builder"

    val nodeName: String = element.typeName.removePrefix("Ir")

    val ref: ClassRef<NamedTypeParameterRef> = ClassRef(TypeKind.Class, PACKAGE, typeName)

    /** A builder of an element without implementations only declares properties for its subclasses. */
    val isAbstract: Boolean get() = element.implementations.isEmpty()

    /** Whether it has subclasses although it isn't abstract, like the element `IrBlock` with `IrReturnableBlock`. */
    var isOpen: Boolean = false

    var kDoc: String? = null

    val annotations: MutableList<Expr> = mutableListOf()

    /**
     * Fields that define the node, e.g. the referenced symbol: they are read-only properties of the builder, and parameters
     * of its constructor and of the entry points, so that defaults can be derived from them (the type of a call is the return type
     * of the callee). By default, these are the immutable fields and the referenced and declared symbols named `symbol`.
     */
    val identityFields: MutableSet<String> = mutableSetOf()

    /** Fields of [element] that aren't builder properties: derived from other fields, or not supported yet. */
    val excludedFields: MutableSet<String> = mutableSetOf()

    /** `lateinit` properties with an `internal val is<Name>Initialized`, for the hooks that apply defaults. */
    val initializationChecks: MutableSet<String> = mutableSetOf()

    /** Defaults of the properties that can't be derived from the type of the field. */
    val defaults: MutableMap<String, Expr> = linkedMapOf()

    /**
     * Defaults derived from the [identityFields], assigned in the `init` block of the builder:
     * they are computed when the builder is created, so the builder lambda sees them and can change them.
     */
    val initialValues: MutableMap<String, Expr> = linkedMapOf()

    /** A hand-written `internal fun <name>(builder)` called at the end of the `init` block. */
    var initHook: String? = null

    /**
     * Defaults that need the context of an `IrBuilder` (built-ins, the scope),
     * assigned by the `ir<Node>` entry point. They are expressions with the `IrBuilder` as `this`.
     */
    val contextValues: MutableMap<String, Expr> = linkedMapOf()

    /** A hand-written `internal fun <Receiver>.<name>(builder)` called after the [contextValues] are assigned. */
    var contextHook: String? = null

    val build: ExpressionBuildConfig = ExpressionBuildConfig()

    /** The entry points of a builder that isn't abstract. */
    val entryPoints: MutableSet<ExpressionEntryPoint> = linkedSetOf(ExpressionEntryPoint.ContextFree, ExpressionEntryPoint.IrBuilder)

    val ancestors: List<ExpressionBuilder>
        get() = generateSequence(parent) { it.parent }.toList()

    companion object {
        const val PACKAGE = "org.jetbrains.kotlin.ir.builders.expressions.v2"

        const val JVM_FACADE_NAME = "IrExpressionBuilders"
    }
}

/**
 * How `build(builder)` creates the node: the implementation constructor is called with the fields that are its parameters,
 * the remaining properties are assigned to the node afterwards.
 */
class ExpressionBuildConfig {
    /** The name of the `IrFactory` method creating the node, if it isn't created with the implementation constructor. */
    var factoryMethod: String? = null

    /** A hand-written `internal fun <name>(builder)` called before the node is created, e.g. to derive defaults from other properties. */
    var beforeBuild: String? = null

    /** Properties that aren't assigned to the node by the generated code, but by [afterBuild]. */
    val setByHook: MutableSet<String> = mutableSetOf()

    /** A hand-written `internal fun <name>(builder, node)`, called after the properties are assigned. */
    var afterBuild: String? = null
}

enum class ExpressionEntryPoint {
    /** `build<Node>(identity) {}`: without a context, the offsets are undefined unless set. */
    ContextFree,

    /** `IrBuilder.ir<Node>(identity) {}`: the offsets of the builder, and the [ExpressionBuilder.contextValues]. */
    IrBuilder,

    /** `IrBuilderWithScope.ir<Node>(identity) {}`: like [IrBuilder], when the context values need the scope. */
    IrBuilderWithScope,

    /** `IrFactory.ir<Node> {}`, for bodies that are created by the factory. */
    IrFactory,
}

class ConstructorParameter(
    val field: Field,
    /** Declared by the constructor of the parent builder, which isn't abstract: passed to it as is. */
    val isPassedToParent: Boolean,
    /** Overrides an abstract property of a parent builder. */
    val isOverride: Boolean,
)

/** A property of an expression builder: a field of an IR element as it's declared in a builder. */
class ExpressionBuilderProperty(
    val field: Field,
    /** An identity property is read-only and is a parameter of the constructor and the entry points. */
    val isIdentity: Boolean,
    /** Whether it overrides (narrows) the property of a parent builder: only for identity properties. */
    val isOverride: Boolean,
    /** `null` for a `lateinit` property or an identity property. */
    val default: Expr?,
)
