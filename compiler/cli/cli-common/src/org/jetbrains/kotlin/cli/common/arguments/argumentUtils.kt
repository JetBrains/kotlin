/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.common.arguments

import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

@Suppress("UNCHECKED_CAST")
fun <T : Any> copyBean(bean: T) =
        copyProperties(bean, bean::class.java.newInstance()!!, true, collectProperties(bean::class as KClass<T>, false))

fun <From : Any, To : From> mergeBeans(from: From, to: To): To {
    // TODO: rewrite when updated version of com.intellij.util.xmlb is available on TeamCity
    @Suppress("UNCHECKED_CAST")
    return copyProperties(from, to, false, collectProperties(from::class as KClass<From>, false))
}

@Suppress("UNCHECKED_CAST")
fun <From : Any, To : Any> copyInheritedFields(from: From, to: To) =
        copyProperties(from, to, true, collectProperties(from::class as KClass<From>, true))

@Suppress("UNCHECKED_CAST")
fun <From : Any, To : Any> copyFieldsSatisfying(from: From, to: To, predicate: (KProperty1<From, Any?>) -> Boolean) =
        copyProperties(from, to, true, collectProperties(from::class as KClass<From>, false).filter(predicate))

private fun <From : Any, To : Any> copyProperties(
        from: From,
        to: To,
        deepCopyWhenNeeded: Boolean,
        propertiesToCopy: List<KProperty1<From, Any?>>
): To {
    if (from == to) return to

    for (fromProperty in propertiesToCopy) {
        @Suppress("UNCHECKED_CAST")
        val toProperty = to::class.memberProperties.firstOrNull { it.name == fromProperty.name } as? KMutableProperty1<To, Any?>
                         ?: continue
        val fromValue = fromProperty.get(from)
        toProperty.set(to, if (deepCopyWhenNeeded) fromValue?.copyValueIfNeeded() else fromValue)
    }
    return to
}

private fun Any.copyValueIfNeeded(): Any {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ByteArray -> Arrays.copyOf(this, size)
        is CharArray -> Arrays.copyOf(this, size)
        is ShortArray -> Arrays.copyOf(this, size)
        is IntArray -> Arrays.copyOf(this, size)
        is LongArray -> Arrays.copyOf(this, size)
        is FloatArray -> Arrays.copyOf(this, size)
        is DoubleArray -> Arrays.copyOf(this, size)
        is BooleanArray -> Arrays.copyOf(this, size)

        is Array<*> -> java.lang.reflect.Array.newInstance(this::class.java.componentType, size).apply {
            this as Array<Any?>
            (this@copyValueIfNeeded as Array<Any?>).forEachIndexed { i, value -> this[i] = value?.copyValueIfNeeded() }
        }

        is MutableCollection<*> -> (this as Collection<Any?>).mapTo(this::class.java.newInstance() as MutableCollection<Any?>) { it?.copyValueIfNeeded() }

        is MutableMap<*, *> -> (this::class.java.newInstance() as MutableMap<Any?, Any?>).apply {
            for ((k, v) in this@copyValueIfNeeded.entries) {
                put(k?.copyValueIfNeeded(), v?.copyValueIfNeeded())
            }
        }

        else -> this
    }
}

fun <T : Any> collectProperties(kClass: KClass<T>, inheritedOnly: Boolean): List<KProperty1<T, Any?>> {
    val properties = ArrayList(kClass.memberProperties)
    if (inheritedOnly) {
        properties.removeAll(kClass.declaredMemberProperties)
    }
    return properties.filter {
        it.visibility == KVisibility.PUBLIC && it.findAnnotation<Transient>() == null
    }
}