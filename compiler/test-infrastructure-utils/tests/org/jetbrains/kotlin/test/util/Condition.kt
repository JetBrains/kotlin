/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.util

import java.util.*

fun interface Condition<in T> {
    operator fun invoke(value: T): Boolean
}

object Conditions {
    private val TRUE = Condition<Any?> { true }
    private val FALSE = Condition<Any?> { false }

    fun <T> alwaysTrue(): Condition<T> {
        return TRUE
    }

    fun <T> alwaysFalse(): Condition<T> {
        return FALSE
    }

    fun <T> notNull(): Condition<T> {
        return Condition { it != null }
    }

    fun <T> constant(value: Boolean): Condition<T> {
        return if (value) alwaysTrue() else alwaysFalse()
    }

    fun <T> instanceOf(clazz: Class<*>): Condition<T> {
        return Condition { t -> clazz.isInstance(t) }
    }

    fun <T> notInstanceOf(clazz: Class<*>): Condition<T> {
        return Condition { t -> !clazz.isInstance(t) }
    }

    fun assignableTo(clazz: Class<*>): Condition<Class<*>> {
        return Condition { t -> clazz.isAssignableFrom(t) }
    }

    fun <T> instanceOf(vararg clazz: Class<*>): Condition<T> {
        return Condition { t ->
            for (aClass in clazz) {
                if (aClass.isInstance(t)) return@Condition true
            }
            false
        }
    }

    fun <T> `is`(option: T): Condition<T> {
        return equalTo(option)
    }

    fun <T> equalTo(option: Any?): Condition<T> {
        return Condition { t -> t == option }
    }

    fun <T> notEqualTo(option: Any?): Condition<T> {
        return Condition { t -> t != option }
    }

    fun <T> oneOf(vararg options: T): Condition<T> {
        return oneOf(options.toList())
    }

    fun <T> oneOf(options: Collection<T>): Condition<T> {
        return Condition { t -> options.contains(t) }
    }

    fun <T> not(c: Condition<T>): Condition<T> {
        if (c === alwaysTrue<Any>()) return alwaysFalse()
        if (c === alwaysFalse<Any>()) return alwaysTrue()
        return if (c is Not<*>) {
            (c as Not<T>).c as Condition<T>
        } else Not(c)
    }

    fun <T> and(c1: Condition<T>, c2: Condition<T>): Condition<T> {
        if (c1 === alwaysTrue<Any>() || c2 === alwaysFalse<Any>()) return c2
        return if (c2 === alwaysTrue<Any>() || c1 === alwaysFalse<Any>()) c1 else And(c1, c2)
    }

    fun <T> or(c1: Condition<T>, c2: Condition<T>): Condition<T> {
        if (c1 === alwaysFalse<Any>() || c2 === alwaysTrue<Any>()) return c2
        return if (c2 === alwaysFalse<Any>() || c1 === alwaysTrue<Any>()) c1 else Or(c1, c2)
    }

    fun <A, B> compose(func: (A) -> B, condition: Condition<B>): Condition<A> {
        return Condition { condition.invoke(func(it)) }
    }

    fun <T> cached(c: Condition<T>): Condition<T> {
        return SoftRefCache(c)
    }

    private class Not<T>(val c: Condition<T>) : Condition<T> {
        override fun invoke(value: T): Boolean {
            return !c.invoke(value)
        }
    }

    private class And<T>(val c1: Condition<T>, val c2: Condition<T>) : Condition<T> {
        override fun invoke(value: T): Boolean {
            return c1.invoke(value) && c2.invoke(value)
        }
    }

    private class Or<T>(val c1: Condition<T>, val c2: Condition<T>) : Condition<T> {
        override fun invoke(value: T): Boolean {
            return c1.invoke(value) || c2.invoke(value)
        }
    }

    private class SoftRefCache<T>(private val myCondition: Condition<T>) : Condition<T> {
        private val myCache: WeakHashMap<T, Boolean> = WeakHashMap();

        override fun invoke(value: T): Boolean {
            return myCache.computeIfAbsent(value) { myCondition(value) }
        }
    }
}

infix fun <T> Condition<T>.and(other: Condition<T>): Condition<T> {
    return Conditions.and(this, other)
}

infix fun <T> Condition<T>.or(other: Condition<T>): Condition<T> {
    return Conditions.or(this, other)
}

operator fun <T> Condition<T>.not(): Condition<T> {
    return Conditions.not(this)
}

fun <T> Condition<T>.cached(): Condition<T> {
    return Conditions.cached(this)
}
