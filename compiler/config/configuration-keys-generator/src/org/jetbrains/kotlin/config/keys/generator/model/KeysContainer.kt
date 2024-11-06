/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator.model

import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

sealed class Key {
    abstract val name: String
    abstract val description: String
    abstract val importsToAdd: List<String>
    abstract val accessorName: String
    abstract val comment: String?

    abstract val typeString: String
    abstract val types: List<KType>

    val capitalizedAccessorName: String
        get() = accessorName.capitalizeAsciiOnly()
}

class SimpleKey(
    override val name: String,
    override val description: String,
    val type: KType,
    val defaultValue: String?,
    override val importsToAdd: List<String>,
    override val accessorName: String,
    override val comment: String?,
) : Key() {
    override val typeString: String
        get() = type.name

    override val types: List<KType>
        get() = listOf(type)
}

sealed class CollectionKey : Key() {
    val mutableTypeString: String
        get() = "Mutable$typeString"
}

class ListKey(
    override val name: String,
    override val description: String,
    val elementType: KType,
    override val importsToAdd: List<String>,
    override val accessorName: String,
    override val comment: String?,
) : CollectionKey() {
    override val typeString: String
        get() = "List<${elementType.name}>"

    override val types: List<KType>
        get() = listOf(elementType)
}

class MapKey(
    override val name: String,
    override val description: String,
    val keyType: KType,
    val valueType: KType,
    override val importsToAdd: List<String>,
    override val accessorName: String,
    override val comment: String?,
) : CollectionKey() {
    override val typeString: String
        get() = "Map<${keyType.name}, ${valueType.name}>"

    override val types: List<KType>
        get() = listOf(keyType, valueType)
}

class DeprecatedKey(
    override val name: String,
    val type: KType,
    override val importsToAdd: List<String>,
    override val comment: String?,
    val deprecation: Deprecated,
    val initializer: String,
) : Key() {
    override val description: String
        get() = shouldNotBeCalled()
    override val accessorName: String
        get() = shouldNotBeCalled()
    override val typeString: String
        get() = type.name
    override val types: List<KType>
        get() = listOf(type)
}

abstract class KeysContainer(val packageName: String, val className: String) {

    @PrivateForInline
    @PublishedApi
    internal val _keys = mutableListOf<Key>()

    @OptIn(PrivateForInline::class)
    val keys: List<Key> = _keys

    @OptIn(PrivateForInline::class)
    inline fun <reified T : Any> key(
        description: String,
        defaultValue: String? = null,
        importsToAdd: List<String> = emptyList(),
        accessorName: String? = null,
        comment: String? = null,
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, Key>> {
        return PropertyDelegateProvider { _, property ->
            val name = property.name
            val type = typeOf<T>()
            val key = when (T::class.qualifiedName) {
                "kotlin.collections.List" -> ListKey(
                    name,
                    description,
                    elementType = type.arguments[0].type!!,
                    importsToAdd,
                    accessorName ?: name.toCamelCase(),
                    comment,
                )
                "kotlin.collections.Map" -> {
                    MapKey(
                        name,
                        description,
                        keyType = type.arguments[0].type!!,
                        valueType = type.arguments[1].type!!,
                        importsToAdd,
                        accessorName ?: name.toCamelCase(),
                        comment,
                    )
                }
                else -> SimpleKey(
                    name,
                    description,
                    type,
                    defaultValue,
                    importsToAdd,
                    accessorName ?: name.toCamelCase(),
                    comment,
                )
            }
            _keys += key
            ReadOnlyProperty<Any?, Key> { _, _ -> key }
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <reified T : Any> deprecatedKey(
        initializer: String,
        deprecation: Deprecated,
        importsToAdd: List<String> = emptyList(),
        comment: String? = null,
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, Key>> {
        return PropertyDelegateProvider { _, property ->
            val name = property.name
            val klass = T::class
            val key = DeprecatedKey(
                name,
                klass.starProjectedType,
                importsToAdd,
                comment,
                deprecation,
                initializer
            )
            _keys += key
            ReadOnlyProperty<Any?, Key> { _, _ -> key }
        }
    }


    fun String.toCamelCase(): String {
        val segments = this.split("_").map { it.toLowerCaseAsciiOnly() }
        return buildString {
            append(segments.first())
            segments.subList(1, segments.size).forEach { append(it.capitalizeAsciiOnly()) }
        }
    }
}


val KType.name: String
    get() = (classifier as KClass<*>).simpleName!!
