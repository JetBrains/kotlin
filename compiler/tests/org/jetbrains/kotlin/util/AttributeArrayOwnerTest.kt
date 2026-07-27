/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Behavioral tests for the [ArrayMap] escalation ladder used by [AttributeArrayOwner]:
 * `EmptyArrayMap` (0) -> `OneElementArrayMap` (1) -> `TinyArrayMap` (2-4) -> `ArrayMapImpl` (5+).
 *
 * The concrete map classes are `internal` to `core/compiler.common`, so the ladder is exercised through the public
 * owner API and the implementation is checked via the runtime class name of the (protected) backing map. See KT-87817.
 */
class AttributeArrayOwnerTest {
    private interface Key
    private class K0 : Key
    private class K1 : Key
    private class K2 : Key
    private class K3 : Key
    private class K4 : Key
    private class K5 : Key

    private object Registry : TypeRegistry<Key, Any>() {
        override fun ConcurrentHashMap<String, Int>.customComputeIfAbsent(key: String, compute: (String) -> Int): Int =
            computeIfAbsent(key, compute)
    }

    private class Owner : AttributeArrayOwner<Key, Any> {
        constructor() : super()
        constructor(map: ArrayMap<Any>) : super(map)

        override val typeRegistry: TypeRegistry<Key, Any> get() = Registry

        val size: Int get() = arrayMap.size
        val implName: String get() = arrayMap::class.simpleName!!

        fun put(key: KClass<out Key>, value: Any) = registerComponent(key, value)
        fun putAll(vararg entries: Pair<KClass<out Key>, Any>) {
            for ((first, second) in entries) put(first, second)
        }

        fun remove(key: KClass<out Key>) = removeComponent(key)
        operator fun get(key: KClass<out Key>): Any? = this[typeRegistry.getId(key.qualifiedName!!)]
        fun copyOwner(): Owner = Owner(arrayMap.copy())
        fun valuesInOrder(): List<Any> = toList()
    }

    @Test
    fun escalationLadder() {
        val o = Owner()
        assertEquals(0, o.size)
        assertEquals("EmptyArrayMap", o.implName)

        o.put(K0::class, "v0")
        assertEquals(1, o.size)
        assertEquals("OneElementArrayMap", o.implName)

        o.put(K1::class, "v1")
        assertEquals(2, o.size)
        assertEquals("TinyArrayMap", o.implName)

        o.put(K2::class, "v2")
        assertEquals(3, o.size)
        assertEquals("TinyArrayMap", o.implName)

        o.put(K3::class, "v3")
        assertEquals(4, o.size)
        assertEquals("TinyArrayMap", o.implName)

        o.put(K4::class, "v4")
        assertEquals(5, o.size)
        assertEquals("ArrayMapImpl", o.implName)

        assertEquals("v0", o[K0::class])
        assertEquals("v1", o[K1::class])
        assertEquals("v2", o[K2::class])
        assertEquals("v3", o[K3::class])
        assertEquals("v4", o[K4::class])
        assertNull(o[K5::class])

        assertEquals(setOf("v0", "v1", "v2", "v3", "v4"), o.valuesInOrder().toSet())
    }

    @Test
    fun iterationIsInsertionOrder() {
        val o = Owner()
        o.put(K0::class, "a")
        o.put(K1::class, "b")
        o.put(K2::class, "c")
        // TinyArrayMap fills slots front-to-back, so iteration preserves insertion order
        // (relied on by ConeAttributes.transformTypesWith).
        assertEquals(listOf("a", "b", "c"), o.valuesInOrder())

        o.put(K1::class, "B") // overwrite keeps the slot position
        assertEquals(listOf("a", "B", "c"), o.valuesInOrder())
    }

    @Test
    fun overwriteKeepsSizeAndImpl() {
        val o = Owner()

        // OneElementArrayMap
        o.put(K0::class, "a")
        o.put(K0::class, "A")
        assertEquals(1, o.size)
        assertEquals("OneElementArrayMap", o.implName)
        assertEquals("A", o[K0::class])

        // TinyArrayMap
        o.put(K1::class, "b")
        o.put(K2::class, "c")
        o.put(K1::class, "B")
        assertEquals(3, o.size)
        assertEquals("TinyArrayMap", o.implName)
        assertEquals("A", o[K0::class])
        assertEquals("B", o[K1::class])
        assertEquals("c", o[K2::class])

        // ArrayMapImpl
        o.put(K3::class, "d")
        o.put(K4::class, "e")
        o.put(K3::class, "D")
        assertEquals(5, o.size)
        assertEquals("ArrayMapImpl", o.implName)
        assertEquals("D", o[K3::class])
    }

    @Test
    fun removalKeepsArrayMapImplThenDowngrades() {
        val o = Owner()
        o.putAll(K0::class to "v0", K1::class to "v1", K2::class to "v2", K3::class to "v3", K4::class to "v4")
        assertEquals(5, o.size)
        assertEquals("ArrayMapImpl", o.implName)

        // An ArrayMapImpl that shrinks below 5 stays an ArrayMapImpl (this is the case that would crash
        // size-based dispatch, since size 2-4 no longer implies TinyArrayMap).
        o.remove(K4::class)
        assertEquals(4, o.size)
        assertEquals("ArrayMapImpl", o.implName)
        assertNull(o[K4::class])

        o.remove(K3::class)
        assertEquals(3, o.size)
        assertEquals("ArrayMapImpl", o.implName)

        o.remove(K2::class)
        assertEquals(2, o.size)
        assertEquals("ArrayMapImpl", o.implName)

        o.remove(K1::class)
        assertEquals(1, o.size)
        assertEquals("OneElementArrayMap", o.implName)

        o.remove(K0::class)
        assertEquals(0, o.size)
        assertEquals("EmptyArrayMap", o.implName)
        assertNull(o[K0::class])
    }

    @Test
    fun removalFromTinyDowngrades() {
        val o = Owner()
        o.put(K0::class, "a")
        o.put(K1::class, "b")
        o.put(K2::class, "c")

        o.remove(K1::class)
        assertEquals(2, o.size)
        assertEquals("TinyArrayMap", o.implName)
        assertNull(o[K1::class])
        assertEquals("a", o[K0::class])
        assertEquals("c", o[K2::class])

        o.remove(K0::class)
        assertEquals(1, o.size)
        assertEquals("OneElementArrayMap", o.implName)
        assertEquals("c", o[K2::class])

        o.remove(K2::class)
        assertEquals(0, o.size)
        assertEquals("EmptyArrayMap", o.implName)
    }

    @Test
    fun copyOfTinyIsIndependent() {
        val orig = Owner()
        orig.put(K0::class, "a")
        orig.put(K1::class, "b")
        orig.put(K2::class, "c")

        val copy = orig.copyOwner()

        // Mutate the original in several ways.
        orig.put(K3::class, "d")
        orig.remove(K0::class)
        orig.put(K0::class, "A")

        // The copy must be untouched.
        assertEquals(3, copy.size)
        assertEquals("TinyArrayMap", copy.implName)
        assertEquals("a", copy[K0::class])
        assertEquals("b", copy[K1::class])
        assertEquals("c", copy[K2::class])
        assertNull(copy[K3::class])
    }

    @Test
    fun copyOfArrayMapImplIsIndependent() {
        val orig = Owner()
        orig.putAll(K0::class to "v0", K1::class to "v1", K2::class to "v2", K3::class to "v3", K4::class to "v4")
        assertEquals("ArrayMapImpl", orig.implName)

        val copy = orig.copyOwner()

        // ArrayMapImpl is mutated in place, so the copy must have its own backing array.
        orig.put(K5::class, "v5")
        orig.remove(K0::class)

        assertEquals(5, copy.size)
        assertEquals("v0", copy[K0::class])
        assertEquals("v4", copy[K4::class])
        assertNull(copy[K5::class])
    }
}
