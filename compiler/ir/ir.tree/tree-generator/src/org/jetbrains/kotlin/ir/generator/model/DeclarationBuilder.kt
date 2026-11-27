/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import org.jetbrains.kotlin.generators.tree.ClassRef
import org.jetbrains.kotlin.generators.tree.PositionTypeParameterRef
import org.jetbrains.kotlin.generators.tree.TypeKind
import org.jetbrains.kotlin.generators.tree.TypeRef
import org.jetbrains.kotlin.generators.tree.imports.Importable
import org.jetbrains.kotlin.generators.tree.type

class Expr(val text: String, val imports: List<Importable> = emptyList())

/**
 * The header properties of a builder are the fields of [element] that aren't fields of the elements of the [parent] builders,
 * except the ones that are excluded or are children (see [org.jetbrains.kotlin.ir.generator.DeclarationBuilderConfigurator]).
 */
class DeclarationBuilder(
    val typeName: String,
    val element: Element,
    val parent: DeclarationBuilder?,
    val isAbstract: Boolean,
) {
    val ref: ClassRef<PositionTypeParameterRef> = type(PACKAGE, typeName, TypeKind.Class)

    /** The name of the file it's printed to, without the extension. */
    var fileName: String = typeName

    var kDoc: String? = null

    val annotations: MutableList<Expr> = mutableListOf()

    /** Fields of [element] that aren't builder properties: set by the owner, derived, or not supported yet. */
    val excludedFields: MutableSet<String> = mutableSetOf()

    /** Child fields of [element] that are nevertheless plain properties (e.g. the initializer of a variable). */
    val childFieldsAsProperties: MutableSet<String> = mutableSetOf()

    /** Properties that are nullable in the builder even though the field isn't, because the default is computed when it's built. */
    val nullableInBuilder: MutableSet<String> = mutableSetOf()

    /**
     * `lateinit` properties with an `internal val is<Name>Initialized`, for the hand-written code that applies defaults
     * derived from the owner or from other properties when the declaration is built.
     */
    val initializationChecks: MutableSet<String> = mutableSetOf()

    /** Header properties that are `abstract val`s here, and are defined by the subclasses (see [fieldOverrides]). */
    val abstractFields: MutableSet<String> = mutableSetOf()

    /** Definitions of the [abstractFields] of the parents. */
    val fieldOverrides: MutableList<FieldOverride> = mutableListOf()

    /** Properties that aren't fields of the IR element. */
    val extraProperties: MutableList<ExtraProperty> = mutableListOf()

    /** Values assigned in `init`, i.e. defaults of the properties of the parents that differ for this builder. */
    val initialValues: MutableMap<String, Expr> = linkedMapOf()

    /** The symbol is allocated when the builder is created, so that the declaration can be referenced before it's built. */
    var preallocatesSymbol: Boolean = false

    val children: MutableList<Child> = mutableListOf()

    var build: BuildConfig? = null

    /**
     * The inline entry points creating the builder, e.g. `IrFactory.irClass {}`.
     */
    val entryPoints: MutableList<DeclarationEntryPoint> = mutableListOf()

    /** The `@PublishedApi` functions creating the builder with defaults, used by [entryPoints] and [ChildFunction]s. */
    val creators: MutableList<BuilderCreator> = mutableListOf()

    /**
     * Whether the builder is created by an inline entry point: then its constructor and `build` are `@PublishedApi`,
     * they are the only part of the builder machinery that is inlined into the client code.
     */
    val isCreatedByEntryPoint: Boolean
        get() = entryPoints.isNotEmpty()

    /** Whether the entry points or the child functions of the builder declare `callsInPlace` contracts of their lambdas. */
    val printsContracts: Boolean
        get() = entryPoints.isNotEmpty() || children.any { it is Child.Built && it.functions.isNotEmpty() }

    /**
     * Hand-written public members, e.g. convenience overloads of child functions. They are members, not extensions,
     * so that they're available in builder lambdas without imports.
     */
    val additionalMembers: MutableList<Expr> = mutableListOf()

    val ancestors: List<DeclarationBuilder>
        get() = generateSequence(parent) { it.parent }.toList()

    companion object {
        const val PACKAGE = "org.jetbrains.kotlin.ir.builders.declarations.v2"

        /** The multifile facade of all top-level declarations in [PACKAGE], so that moving them between files is binary compatible. */
        const val JVM_FACADE_NAME = "IrDeclarationBuilders"
    }
}

class FieldOverride(val name: String, val isMutable: Boolean, val value: Expr)

class ExtraProperty(val name: String, val type: TypeRef, val initializer: Expr)

/** A header property: a field of an IR element as it's declared in a builder. */
class HeaderProperty(
    val field: Field,
    val type: TypeRef,
    /** `null` for a `lateinit` property. */
    val default: Expr?,
    val isAbstract: Boolean,
)

sealed class Child(val propertyName: String) {
    /**
     * Children built by builders: each of [functions] creates a builder, applies the lambda and stores it.
     * They are built by the owner's `build` (see [BuildConfig.afterBuild]).
     */
    class Built(
        propertyName: String,
        val isList: Boolean,
        val storedType: DeclarationBuilder,
        val functions: List<ChildFunction>,
    ) : Child(propertyName)

    /** Ready nodes, e.g. members of a class. */
    class Nodes(
        propertyName: String,
        val nodeType: TypeRef,
        val functionName: String,
    ) : Child(propertyName)
}

class ChildFunction(
    val name: String,
    val builder: DeclarationBuilder,
    val kDoc: String? = null,
    val creator: BuilderCreator? = null,
    val hasDefaultLambda: Boolean = false,
    val returnsSymbol: Boolean = true,
    /** For a single child: the message of the error when it's declared twice; `null` means it's overwritten. */
    val alreadyDeclaredMessage: String? = null,
)

/**
 * An inline entry point `<receiver>.<name>(builder: Builder.() -> Unit): Node`: it creates the builder (with [creator], or with
 * its constructor), applies the lambda and passes the builder to [build].
 */
class DeclarationEntryPoint(
    val receiver: TypeRef,
    val name: String,
    /**
     * The function the builder is passed to: the generated `IrFactory.build`, or a hand-written `@PublishedApi`
     * `<receiver>.<build>(builder)` that applies what depends on the owner, e.g. `IrClass.buildMemberFunction`.
     */
    val build: String,
    val creator: BuilderCreator? = null,
    /** Whether the lambda may be omitted. */
    val hasDefaultLambda: Boolean = false,
    val kDoc: String? = null,
)

/**
 * A `@PublishedApi` function `[<receiver>.]<name>()` creating a builder with defaults that differ from the ones of the builder itself:
 * the offsets of an `IrBuilder` receiver, and [values] defined by the owner.
 */
class BuilderCreator(
    val name: String,
    val receiver: TypeRef? = null,
    /** Whether the offsets are taken from the receiver, an `IrBuilder`. */
    val offsetsFromReceiver: Boolean = false,
    val values: Map<String, Expr> = emptyMap(),
)

/**
 * How `IrFactory.build(builder)` creates the node: [creation] is called with the fields that are parameters of the
 * implementation constructor, the remaining header properties are assigned to the node afterwards.
 */
class BuildConfig(val creation: Creation) {
    /** Header properties passed to [creation] even though they aren't parameters of the implementation constructor. */
    val extraArguments: MutableSet<String> = mutableSetOf()

    /** Parameters of `build` that are passed to [creation] as is, e.g. the index of a type parameter. */
    val extraParameters: MutableList<Pair<String, TypeRef>> = mutableListOf()

    /** A hand-written `IrFactory.<name>(builder)`, called before the node is created. */
    var beforeBuild: String? = null

    /** A hand-written `IrFactory.<name>(builder, node)`, called after the header is set, e.g. to build the children. */
    var afterBuild: String? = null
}

sealed class Creation {
    class FactoryMethod(val name: String) : Creation()
    class Function(val function: Importable) : Creation()
}
