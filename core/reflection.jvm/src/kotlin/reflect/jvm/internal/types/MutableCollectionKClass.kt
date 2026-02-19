/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.internal.KTypeParameterImpl
import kotlin.reflect.jvm.internal.KTypeParameterOwnerImpl
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.internal.StandardKTypes

/**
 * A [kotlin.reflect.KClass] implementation for mutable collection classes (i.e. `kotlin.collections.MutableList`).
 *
 * Currently, this class is only used in the type checker implementation for kotlin-reflect,
 * but one day it should probably be used to implement KT-11754.
 *
 * @param klass the read-only collection class (i.e. `kotlin.collections.List`)
 */
internal class MutableCollectionKClass<T : Any>(
    val klass: KClass<T>,
    override val qualifiedName: String,
    createTypeParameters: (MutableCollectionKClass<T>) -> List<KTypeParameter>,
    createSupertypes: (MutableCollectionKClass<T>) -> List<KType>,
) : KClass<T> by klass, TypeConstructorMarker, KTypeParameterOwnerImpl {
    override val typeParameters: List<KTypeParameter> =
        createTypeParameters(this)

    override val supertypes: List<KType> =
        createSupertypes(this)

    override val simpleName: String
        get() = qualifiedName.substringAfterLast(".")

    override fun equals(other: Any?): Boolean =
        other is MutableCollectionKClass<*> && klass == other.klass

    override fun hashCode(): Int =
        klass.hashCode()

    override fun toString(): String =
        "MutableCollectionKClass($klass)"
}

/**
 * At the moment we're hardcoding all known mutable/readonly collection classes and their type parameters with upper bounds.
 * Eventually, it should be refactored to read (and cache somewhere) builtins metadata from `.kotlin_builtins` files, like this:
 *
 *     val builtins = classLoader.getResourceAsStream("kotlin/collections/collections.kotlin_builtins")!!
 *     val metadata = KotlinCommonMetadata.read(builtins)
 *     ... // Find the needed class, and extract supertypes and type parameters from there.
 *
 * Until then, here are the brief declarations of mutable collection classes that we're hardcoding below:
 *
 *     MutableIterable<out T> : Iterable<T>
 *     MutableIterator<out T> : Iterator<T>
 *     MutableCollection<E> : Collection<E>, MutableIterable<E>
 *     MutableList<E> : List<E>, MutableCollection<E>
 *     MutableSet<E> : Set<E>, MutableCollection<E>
 *     MutableListIterator<T> : ListIterator<T>, MutableIterator<T>
 *     MutableMap<K, V> : Map<K, V>
 *     MutableEntry<K, V> : Map.Entry<K, V>
 */
internal fun getMutableCollectionKClass(mutableFqName: FqName, readonlyKClass: KClass<*>): MutableCollectionKClass<*> {
    val klass = MutableCollectionKClass(
        readonlyKClass,
        mutableFqName.asString(),
        createTypeParameters = { klass ->
            readonlyKClass.typeParameters.map { readonlyTypeParameter ->
                KTypeParameterImpl(
                    klass,
                    readonlyTypeParameter.name,
                    variance = if (mutableFqName == StandardNames.FqNames.mutableIterable ||
                        mutableFqName == StandardNames.FqNames.mutableIterator
                    ) KVariance.OUT else KVariance.INVARIANT,
                    isReified = false,
                ).apply {
                    upperBounds = listOf(StandardKTypes.NULLABLE_ANY)
                }
            }
        },
        createSupertypes = { klass ->
            val mutableSuperInterface = when (mutableFqName) {
                StandardNames.FqNames.mutableCollection -> mutableClassOf<MutableIterable<*>>()
                StandardNames.FqNames.mutableList -> mutableClassOf<MutableCollection<*>>()
                StandardNames.FqNames.mutableSet -> mutableClassOf<MutableCollection<*>>()
                StandardNames.FqNames.mutableListIterator -> mutableClassOf<MutableIterator<*>>()
                else -> null
            }
            val typeArguments = klass.typeParameters.map { KTypeProjection.invariant(it.createType()) }
            listOfNotNull(readonlyKClass, mutableSuperInterface).map { it.createType(typeArguments) }
        },
    )
    return klass
}

private inline fun <reified T> mutableClassOf(): KClass<*> =
    (typeOf<T>() as AbstractKType).mutableCollectionClass
        ?: throw KotlinReflectionInternalError("No mutable collection class found: ${T::class}")
