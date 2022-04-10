/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("KAnnotatedElements")

package kotlin.reflect.full

import java.lang.reflect.Method
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass

/**
 * Returns an annotation of the given type on this element.
 */
@SinceKotlin("1.1")
inline fun <reified T : Annotation> KAnnotatedElement.findAnnotation(): T? =
    @Suppress("UNCHECKED_CAST")
    annotations.firstOrNull { it is T } as T?

/**
 * Returns true if this element is annotated with an annotation of type [T].
 */
@SinceKotlin("1.4")
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@WasExperimental(ExperimentalStdlibApi::class)
inline fun <reified T : Annotation> KAnnotatedElement.hasAnnotation(): Boolean =
    findAnnotation<T>() != null

/**
 * Returns all annotations of the given type on this element, including individually applied annotations
 * as well as repeated annotations.
 *
 * In case the annotation is repeated, instances are extracted from the container annotation class similarly to how it happens
 * in Java reflection ([java.lang.reflect.AnnotatedElement.getAnnotationsByType]). This is supported both for Kotlin-repeatable
 * ([kotlin.annotation.Repeatable]) and Java-repeatable ([java.lang.annotation.Repeatable]) annotation classes.
 */
@SinceKotlin("1.7")
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@WasExperimental(ExperimentalStdlibApi::class)
inline fun <reified T : Annotation> KAnnotatedElement.findAnnotations(): List<T> =
    findAnnotations(T::class)

/**
 * Returns all annotations of the given type on this element, including individually applied annotations
 * as well as repeated annotations.
 *
 * In case the annotation is repeated, instances are extracted from the container annotation class similarly to how it happens
 * in Java reflection ([java.lang.reflect.AnnotatedElement.getAnnotationsByType]). This is supported both for Kotlin-repeatable
 * ([kotlin.annotation.Repeatable]) and Java-repeatable ([java.lang.annotation.Repeatable]) annotation classes.
 */
@SinceKotlin("1.7")
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@WasExperimental(ExperimentalStdlibApi::class)
fun <T : Annotation> KAnnotatedElement.findAnnotations(klass: KClass<T>): List<T> {
    val filtered = annotations.filterIsInstance(klass.java)
    if (filtered.isNotEmpty()) return filtered

    val containerClass = Java8RepeatableContainerLoader.loadRepeatableContainer(klass.java)
    if (containerClass != null) {
        val container = annotations.firstOrNull { it.annotationClass.java == containerClass }
        if (container != null) {
            // A repeatable annotation container must have a method "value" returning the array of repeated annotations.
            val valueMethod = container::class.java.getMethod("value")
            @Suppress("UNCHECKED_CAST")
            return (valueMethod(container) as Array<T>).asList()
        }
    }

    return emptyList()
}

@Suppress("UNCHECKED_CAST")
private object Java8RepeatableContainerLoader {
    class Cache(val repeatableClass: Class<out Annotation>?, val valueMethod: Method?)

    var cache: Cache? = null

    private fun buildCache(): Cache {
        val repeatableClass = try {
            Class.forName("java.lang.annotation.Repeatable") as Class<out Annotation>
        } catch (e: ClassNotFoundException) {
            return Cache(null, null)
        }

        return Cache(repeatableClass, repeatableClass.getMethod("value"))
    }

    fun loadRepeatableContainer(klass: Class<out Annotation>): Class<out Annotation>? {
        val cache = cache ?: synchronized(this) {
            cache ?: buildCache().also { cache = it }
        }

        val repeatableClass = cache.repeatableClass ?: return null
        val repeatable = klass.getAnnotation(repeatableClass) ?: return null
        val valueMethod = cache.valueMethod ?: return null

        return valueMethod(repeatable) as Class<out Annotation>
    }
}
