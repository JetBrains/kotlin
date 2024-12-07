/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

import kotlin.reflect.KDeclarationContainer


private val K_CLASS_CACHE = createCache { KClassImpl(it) }
private val K_PACKAGE_CACHE = createCache { KPackageImpl(it) }

// This function is invoked on each reflection access to Java classes, properties, etc. Performance is critical here.
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> getOrCreateKotlinClass(jClass: Class<T>): KClassImpl<T> = K_CLASS_CACHE.get(jClass) as KClassImpl<T>

internal fun <T : Any> getOrCreateKotlinPackage(jClass: Class<T>): KDeclarationContainer = K_PACKAGE_CACHE.get(jClass)

internal fun clearCaches() {
    K_CLASS_CACHE.clear()
    K_PACKAGE_CACHE.clear()
    CACHE_FOR_BASE_CLASSIFIERS.clear()
    CACHE_FOR_NULLABLE_BASE_CLASSIFIERS.clear()
    CACHE_FOR_GENERIC_CLASSIFIERS.clear()
}

// typeOf-related caches

// Without type arguments and nullability
private val CACHE_FOR_BASE_CLASSIFIERS = createCache {
    getOrCreateKotlinClass(it).createType(emptyList(), false, emptyList())
}

private val CACHE_FOR_NULLABLE_BASE_CLASSIFIERS = createCache {
    getOrCreateKotlinClass(it).createType(emptyList(), true, emptyList())
}

private typealias Key = Pair<List<KTypeProjection>, Boolean>
// Class -> ((type arguments, is nullable) -> type)
private val CACHE_FOR_GENERIC_CLASSIFIERS = createCache<ConcurrentHashMap<Key, KType>> {
    ConcurrentHashMap()
}

internal fun <T : Any> getOrCreateKType(jClass: Class<T>, arguments: List<KTypeProjection>, isMarkedNullable: Boolean): KType {
    return if (arguments.isEmpty()) {
        if (isMarkedNullable) {
            CACHE_FOR_NULLABLE_BASE_CLASSIFIERS.get(jClass)
        } else {
            CACHE_FOR_BASE_CLASSIFIERS.get(jClass)
        }
    } else {
        getOrCreateKTypeWithTypeArguments(jClass, arguments, isMarkedNullable)
    }
}

private fun <T : Any> getOrCreateKTypeWithTypeArguments(
    jClass: Class<T>,
    arguments: List<KTypeProjection>,
    isMarkedNullable: Boolean
): KType {
    val cache = CACHE_FOR_GENERIC_CLASSIFIERS.get(jClass)
    return cache.getOrPut(arguments to isMarkedNullable) {
        getOrCreateKotlinClass(jClass).createType(arguments, isMarkedNullable, emptyList())
    }
}


