/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.util

import com.squareup.kotlinpoet.asClassName
import org.jetbrains.kotlin.types.Variance
import java.util.*
import kotlin.reflect.KClass

interface TypeRef {
    object Star : TypeRef
}

interface ClassOrElementRef : TypeRefWithNullability

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
    val canonicalName: String = if (names[0].isEmpty())
        names.subList(1, names.size).joinToString(".") else
        names.joinToString(".")

    /** Package name, like `"kotlin.collections"` for `Map.Entry`. */
    val packageName: String get() = names[0]

    /** Simple name of this class, like `"Entry"` for `Map.Entry`. */
    val simpleName: String get() = names[names.size - 1]

    /**
     * The enclosing classes, outermost first, followed by the simple name. This is `["Map", "Entry"]`
     * for `Map.Entry`.
     */
    val simpleNames: List<String> get() = names.subList(1, names.size)

    override fun copy(args: Map<P, TypeRef>) = ClassRef(kind, names, args, nullable)
    override fun copy(nullable: Boolean) = ClassRef(kind, names, args, nullable)

    override fun toString() = canonicalName
}

sealed interface TypeParameterRef : TypeRef

data class PositionTypeParameterRef(
    val index: Int
) : TypeParameterRef {
    override fun toString() = index.toString()
}

open class NamedTypeParameterRef(
    val name: String
) : TypeParameterRef {
    override fun equals(other: Any?): Boolean {
        return other is NamedTypeParameterRef && other.name == name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString() = name
}

interface TypeRefWithNullability : TypeRef {
    val nullable: Boolean

    fun copy(nullable: Boolean): TypeRefWithNullability
}

interface ParametrizedTypeRef<Self, P : TypeParameterRef> : TypeRef {
    val args: Map<P, TypeRef>

    fun copy(args: Map<P, TypeRef>): Self
}

fun <T> ParametrizedTypeRef<T, NamedTypeParameterRef>.withArgs(vararg args: Pair<String, TypeRef>) =
    copy(args.associate { (k, v) -> NamedTypeParameterRef(k) to v })

fun <T> ParametrizedTypeRef<T, PositionTypeParameterRef>.withArgs(vararg args: TypeRef) =
    copy(args.withIndex().associate { (i, t) -> PositionTypeParameterRef(i) to t })


class TypeVariable(
    name: String,
    val bounds: List<TypeRef>,
    val variance: Variance
) : NamedTypeParameterRef(name)

fun <P : TypeParameterRef> KClass<*>.asRef(): ClassRef<P> {
    val poet = this.asClassName()
    val kind = if (this.java.isInterface) TypeKind.Interface else TypeKind.Class
    return ClassRef(kind, poet.packageName, *poet.simpleNames.toTypedArray())
}

inline fun <reified T : Any> type() = T::class.asRef<PositionTypeParameterRef>()
inline fun <reified T : Any> refNamed() = T::class.asRef<NamedTypeParameterRef>()
inline fun <reified T : Any> type(vararg args: Pair<String, TypeRef>) = T::class.asRef<NamedTypeParameterRef>().withArgs(*args)
inline fun <reified T : Any> type(vararg args: TypeRef) = T::class.asRef<PositionTypeParameterRef>().withArgs(*args)

fun type(packageName: String, name: String, kind: TypeKind = TypeKind.Interface) =
    ClassRef<PositionTypeParameterRef>(kind, packageName, name)

enum class TypeKind {
    Class,
    Interface
}
