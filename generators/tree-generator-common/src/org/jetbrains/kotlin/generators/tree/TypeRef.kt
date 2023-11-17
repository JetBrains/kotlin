/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.generators.tree.printer.braces
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.joinToWithBuffer
import java.util.*
import kotlin.reflect.KClass

interface TypeRef {

    /**
     * Constructs a new [TypeRef] by recursively replacing referenced [TypeParameterRef]s with other types according to the provided
     * substitution map.
     */
    fun substitute(map: TypeParameterSubstitutionMap): TypeRef

    /**
     * Prints this type to [appendable] with all its arguments and question marks, while recursively collecting
     * `this` and other referenced types into the import collector passed as context.
     */
    context(ImportCollector)
    fun renderTo(appendable: Appendable)

    object Star : TypeRef {

        override fun substitute(map: TypeParameterSubstitutionMap) = this

        override fun toString(): String = "*"

        context(ImportCollector)
        override fun renderTo(appendable: Appendable) {
            appendable.append(toString())
        }
    }
}

/**
 * Prints this type as a string with all its arguments and question marks, while recursively collecting
 * `this` and other referenced types into the import collector passed as context.
 */
context(ImportCollector)
fun TypeRef.render(): String = buildString { renderTo(this) }

sealed interface ClassOrElementRef : TypeRefWithNullability, Importable

// Based on com.squareup.kotlinpoet.ClassName
class ClassRef<P : TypeParameterRef> private constructor(
    val kind: TypeKind,
    names: List<String>,
    override val args: Map<P, TypeRef>,
    override val nullable: Boolean = false,
) : ParametrizedTypeRef<ClassRef<P>, P>, ClassOrElementRef {
    /**
     * Returns a class name created from the given parts. For example, calling this with package name
     * `"java.util"` and simple names `"Map"`, `"Entry"` yields `Map.Entry`.
     */
    constructor(kind: TypeKind, packageName: String, vararg simpleNames: String, args: Map<P, TypeRef> = emptyMap()) :
            this(kind, listOf(packageName, *simpleNames), args) {
        require(simpleNames.isNotEmpty()) { "simpleNames must not be empty" }
        require(simpleNames.none { it.isEmpty() }) {
            "simpleNames must not contain empty items: ${simpleNames.contentToString()}"
        }
    }

    /** From top to bottom. This will be `["java.util", "Map", "Entry"]` for `Map.Entry`. */
    private val names = Collections.unmodifiableList(names)

    /** Fully qualified name using `.` as a separator, like `kotlin.collections.Map.Entry`. */
    val canonicalName: String = if (names[0].isEmpty()) typeName else names.joinToString(".")

    /** Package name, like `"kotlin.collections"` for `Map.Entry`. */
    override val packageName: String
        get() = names[0]

    /** Simple name of this class, like `"Entry"` for `Map.Entry`. */
    val simpleName: String get() = names[names.size - 1]

    override val typeName: String
        get() = simpleNames.joinToString(separator = ".")

    context(ImportCollector)
    override fun renderTo(appendable: Appendable) {
        addImport(this)
        simpleNames.joinTo(appendable, separator = ".")
        renderArgsTo(appendable)
        renderNullabilityTo(appendable)
    }

    /**
     * The enclosing classes, outermost first, followed by the simple name. This is `["Map", "Entry"]`
     * for `Map.Entry`.
     */
    val simpleNames: List<String> get() = names.subList(1, names.size)

    override fun copy(args: Map<P, TypeRef>) = ClassRef(kind, names, args, nullable)
    override fun copy(nullable: Boolean) = ClassRef(kind, names, args, nullable)

    override fun toString() = canonicalName
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassRef<*>) return false
        return kind == other.kind && args == other.args && nullable == other.nullable && names == other.names
    }

    override fun hashCode(): Int = Objects.hash(kind, args, nullable, names)
}

/**
 * Used for specifying a type argument with use-site variance, e.g. `FirClassSymbol<out FirClass>`.
 */
data class TypeRefWithVariance<out T : TypeRef>(val variance: Variance, val typeRef: T) : TypeRef {

    context(ImportCollector)
    override fun renderTo(appendable: Appendable) {
        if (variance != Variance.INVARIANT) {
            appendable.append(variance.label)
            appendable.append(' ')
        }
        typeRef.renderTo(appendable)
    }

    override fun substitute(map: TypeParameterSubstitutionMap): TypeRefWithVariance<*> =
        TypeRefWithVariance(variance, typeRef.substitute(map))
}

interface ElementOrRef<Element> : ParametrizedTypeRef<ElementOrRef<Element>, NamedTypeParameterRef>, ClassOrElementRef
        where Element : AbstractElement<Element, *, *> {
    val element: Element

    override fun copy(nullable: Boolean): ElementRef<Element>
}

data class ElementRef<Element : AbstractElement<Element, *, *>>(
    override val element: Element,
    override val args: Map<NamedTypeParameterRef, TypeRef> = emptyMap(),
    override val nullable: Boolean = false,
) : ElementOrRef<Element> {
    override fun copy(args: Map<NamedTypeParameterRef, TypeRef>) = ElementRef(element, args, nullable)
    override fun copy(nullable: Boolean) = ElementRef(element, args, nullable)

    override val typeName: String
        get() = element.typeName

    override val packageName: String
        get() = element.packageName

    context(ImportCollector)
    override fun renderTo(appendable: Appendable) {
        addImport(element)
        appendable.append(element.typeName)
        renderArgsTo(appendable)
        renderNullabilityTo(appendable)
    }

    override fun toString() = buildString {
        append(element.typeName)
        append("<")
        append(args)
        append(">")
        if (nullable) {
            append("?")
        }
    }
}

data class Lambda(
    val receiver: TypeRefWithNullability?,
    val parameterTypes: List<TypeRefWithNullability> = emptyList(),
    val returnType: TypeRefWithNullability,
    override val nullable: Boolean = false,
) : TypeRefWithNullability {
    override fun substitute(map: TypeParameterSubstitutionMap) =
        Lambda(
            receiver?.substitute(map) as TypeRefWithNullability?,
            parameterTypes.map { it.substitute(map) as TypeRefWithNullability },
            returnType.substitute(map) as TypeRefWithNullability,
            nullable,
        )

    context(ImportCollector)
    override fun renderTo(appendable: Appendable) {
        if (nullable) appendable.append("(")
        receiver?.let {
            it.renderTo(appendable)
            appendable.append('.')
        }
        parameterTypes.joinToWithBuffer(appendable, prefix = "(", postfix = ") -> ") { it.renderTo(this) }
        returnType.renderTo(appendable)
        if (nullable) appendable.append(")?")
    }

    override fun copy(nullable: Boolean) = Lambda(receiver, parameterTypes, returnType, nullable)
}

sealed interface TypeParameterRef : TypeRef, TypeRefWithNullability {
    override fun substitute(map: TypeParameterSubstitutionMap): TypeRef = map[this] ?: this
}

data class PositionTypeParameterRef(
    val index: Int,
    override val nullable: Boolean = false,
) : TypeParameterRef {
    override fun toString() = index.toString()

    context(ImportCollector)
    override fun renderTo(appendable: Appendable) {
        renderingIsNotSupported()
    }

    override fun copy(nullable: Boolean) = PositionTypeParameterRef(index, nullable)
}

open class NamedTypeParameterRef(
    val name: String,
    override val nullable: Boolean = false,
) : TypeParameterRef {
    override fun equals(other: Any?): Boolean {
        return other is NamedTypeParameterRef && other.name == name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString() = name

    context(ImportCollector)
    override fun renderTo(appendable: Appendable) {
        appendable.append(name)
        renderNullabilityTo(appendable)
    }

    final override fun copy(nullable: Boolean) = NamedTypeParameterRef(name, nullable)
}

interface TypeRefWithNullability : TypeRef {
    val nullable: Boolean

    fun copy(nullable: Boolean): TypeRefWithNullability
}

fun TypeRefWithNullability.renderNullabilityTo(appendable: Appendable) {
    if (nullable) {
        appendable.append('?')
    }
}

interface ParametrizedTypeRef<Self : ParametrizedTypeRef<Self, P>, P : TypeParameterRef> : TypeRef {
    val args: Map<P, TypeRef>

    fun copy(args: Map<P, TypeRef>): Self

    override fun substitute(map: TypeParameterSubstitutionMap): Self =
        copy(args.mapValues { it.value.substitute(map) })
}

context(ImportCollector)
fun ParametrizedTypeRef<*, *>.renderArgsTo(appendable: Appendable) {
    if (args.isNotEmpty()) {
        args.values.joinTo(appendable, prefix = "<", postfix = ">") {
            it.renderTo(appendable)
            ""
        }
    }
}

typealias TypeParameterSubstitutionMap = Map<out TypeParameterRef, TypeRef>

fun <Self : ParametrizedTypeRef<Self, NamedTypeParameterRef>> ParametrizedTypeRef<Self, NamedTypeParameterRef>.withArgs(
    vararg args: Pair<String, TypeRef>
) = copy(args.associate { (k, v) -> NamedTypeParameterRef(k) to v })

fun <Self : ParametrizedTypeRef<Self, PositionTypeParameterRef>> ParametrizedTypeRef<Self, PositionTypeParameterRef>.withArgs(
    vararg args: TypeRef
) = copy(args.withIndex().associate { (i, t) -> PositionTypeParameterRef(i) to t })


class TypeVariable(
    name: String,
    val bounds: List<TypeRef> = emptyList(),
    val variance: Variance = Variance.INVARIANT,
) : NamedTypeParameterRef(name)

fun <P : TypeParameterRef> KClass<*>.asRef(): ClassRef<P> {
    val qualifiedName = this.qualifiedName ?: error("$this doesn't have qualified name and thus cannot be converted to ClassRef")
    val kind = if (java.isInterface) TypeKind.Interface else TypeKind.Class
    val parts = qualifiedName.split('.')
    val indexWhereClassNameStarts = parts.indexOfFirst { it.first().isUpperCase() }
    val packageName = parts.take(indexWhereClassNameStarts).joinToString(separator = ".")
    val simpleNames = parts.drop(indexWhereClassNameStarts)
    return ClassRef(kind, packageName, *simpleNames.toTypedArray())
}

inline fun <reified T : Any> type() = T::class.asRef<PositionTypeParameterRef>()
inline fun <reified T : Any> refNamed() = T::class.asRef<NamedTypeParameterRef>()
inline fun <reified T : Any> type(vararg args: Pair<String, TypeRef>) = T::class.asRef<NamedTypeParameterRef>().withArgs(*args)
inline fun <reified T : Any> type(vararg args: TypeRef) = T::class.asRef<PositionTypeParameterRef>().withArgs(*args)

fun type(packageName: String, name: String, kind: TypeKind = TypeKind.Interface) =
    ClassRef<PositionTypeParameterRef>(kind, packageName, name)

val ClassOrElementRef.typeKind: TypeKind
    get() = when (this) {
        is ElementOrRef<*> -> element.kind!!.typeKind
        is ClassRef<*> -> kind
    }

fun ClassOrElementRef.inheritanceClauseParenthesis(): String = when (this) {
    is ElementOrRef<*> -> element.kind.braces()
    is ClassRef<*> -> when (kind) {
        TypeKind.Class -> "()"
        TypeKind.Interface -> ""
    }
}

val TypeRef.nullable: Boolean
    get() = (this as? TypeRefWithNullability)?.nullable ?: false

fun TypeRef.renderingIsNotSupported(): Nothing = error("Rendering is not supported for ${this::class.simpleName}")
