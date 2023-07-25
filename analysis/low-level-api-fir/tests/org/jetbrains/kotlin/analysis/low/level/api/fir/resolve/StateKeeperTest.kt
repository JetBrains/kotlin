/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.StateKeeper
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.entity
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.entityList
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.stateKeeper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StateKeeperTest {
    @Test
    fun testSimple() {
        class Foo(private var value: String) {
            fun getValue() = value
            fun setValue(newValue: String) {
                value = newValue
            }
        }

        val keeper: StateKeeper<Foo, Unit> = stateKeeper { _, _ ->
            add(Foo::getValue, Foo::setValue)
        }

        val foo = Foo("Foo")

        keeper.withRestoration(foo) {
            assertEquals("Foo", foo.getValue())
            foo.setValue("Bar")
            assertEquals("Bar", foo.getValue())
        }

        assertEquals("Foo", foo.getValue())
    }

    @Test
    fun testProperty() {
        class Foo(var value: String)

        val keeper: StateKeeper<Foo, Unit> = stateKeeper { _, _ ->
            add(Foo::value::get, Foo::value::set)
        }

        val foo = Foo("Foo")

        keeper.withRestoration(foo) {
            assertEquals("Foo", foo.value)
            foo.value = "Bar"
            assertEquals("Bar", foo.value)
        }

        assertEquals("Foo", foo.value)
    }

    @Test
    fun testArranger() {
        class Foo(var value: String)

        val keeper: StateKeeper<Foo, Unit> = stateKeeper { _, _ ->
            add(Foo::value::get, Foo::value::set) { "Baz" }
        }

        val foo = Foo("Foo")

        keeper.withRestoration(foo) {
            assertEquals("Baz", foo.value)
            foo.value = "Bar"
            assertEquals("Bar", foo.value)
        }

        assertEquals("Foo", foo.value)
    }

    @Test
    fun testInclusion() {
        open class Foo(var one: String)
        class Bar(one: String, var two: Int) : Foo(one)

        val fooKeeper: StateKeeper<Foo, Unit> = stateKeeper { _, _ ->
            add(Foo::one::get, Foo::one::set)
        }

        val barKeeper: StateKeeper<Bar, Unit> = stateKeeper { _, context ->
            add(fooKeeper, context)
            add(Bar::two::get, Bar::two::set)
        }

        val bar = Bar("Foo", 1)

        barKeeper.withRestoration(bar) {
            assertEquals("Foo", bar.one)
            assertEquals(1, bar.two)
            bar.one = "Bar"
            bar.two = 2
            assertEquals("Bar", bar.one)
            assertEquals(2, bar.two)
        }

        assertEquals("Foo", bar.one)
        assertEquals(1, bar.two)
    }

    @Test
    fun testEntity() {
        data class Foo(var value: Int)
        class Bar(var one: String, var foo: Foo)

        val barKeeper: StateKeeper<Bar, Unit> = stateKeeper { bar, context ->
            add(Bar::one::get, Bar::one::set)
            entity(bar.foo, context) { _, _ ->
                add(Foo::value::get, Foo::value::set)
            }
        }

        val foo = Foo(1)
        val bar = Bar("Foo", foo)

        barKeeper.withRestoration(bar) {
            assertEquals("Foo", bar.one)
            assertEquals(1, bar.foo.value)

            bar.one = "Bar"
            bar.foo.value = 2

            assertEquals("Bar", bar.one)
            assertEquals(2, bar.foo.value)
        }

        assertEquals("Foo", bar.one)
        assertEquals(1, bar.foo.value)
        assert(bar.foo === foo)
    }

    @Test
    fun testEntityInclusion() {
        data class Foo(var value: Int)
        class Bar(var one: String, var foo: Foo)

        val fooKeeper: StateKeeper<Foo, Unit> = stateKeeper { _, _ ->
            add(Foo::value::get, Foo::value::set)
        }

        val barKeeper: StateKeeper<Bar, Unit> = stateKeeper { bar, context ->
            add(Bar::one::get, Bar::one::set)
            entity(bar.foo, fooKeeper, context)
        }

        val foo = Foo(1)
        val bar = Bar("Foo", foo)

        barKeeper.withRestoration(bar) {
            assertEquals("Foo", bar.one)
            assertEquals(1, bar.foo.value)

            bar.one = "Bar"
            bar.foo.value = 2

            assertEquals("Bar", bar.one)
            assertEquals(2, bar.foo.value)
        }

        assertEquals("Foo", bar.one)
        assertEquals(1, bar.foo.value)
        assert(bar.foo === foo)
    }

    @Test
    fun testEntityList() {
        data class Foo(var value: Int)
        class Bar(var one: String, var foos: List<Foo>)

        val barKeeper: StateKeeper<Bar, Unit> = stateKeeper { bar, context ->
            add(Bar::one::get, Bar::one::set)
            entityList(bar.foos, context) { _, _ ->
                add(Foo::value::get, Foo::value::set) { it + 1 }
            }
        }

        val foo0 = Foo(1)
        val foo1 = Foo(2)
        val bar = Bar("Foo", listOf(foo0, foo1))

        barKeeper.withRestoration(bar) {
            assertEquals("Foo", bar.one)
            assertEquals(2, bar.foos[0].value) // +1 because of the arranger
            assertEquals(3, bar.foos[1].value)

            bar.one = "Bar"
            bar.foos[0].value = 100
            bar.foos[1].value = 200

            assertEquals("Bar", bar.one)
            assertEquals(100, bar.foos[0].value)
            assertEquals(200, bar.foos[1].value)
        }

        assertEquals("Foo", bar.one)
        assertEquals(1, bar.foos[0].value)
        assertEquals(2, bar.foos[1].value)
        assert(bar.foos[0] === foo0)
        assert(bar.foos[1] === foo1)
    }

    @Test
    fun testPostProcess() {
        class Foo(var value: String)

        val keeper: StateKeeper<Foo, Unit> = stateKeeper { foo, _ ->
            add(Foo::value::get, Foo::value::set) { "Baz" }

            postProcess {
                foo.value = "Boo"
            }
        }

        val foo = Foo("Foo")

        keeper.withRestoration(foo) {
            assertEquals("Boo", foo.value)
            foo.value = "Bar"
            assertEquals("Bar", foo.value)
        }

        assertEquals("Foo", foo.value)
    }
}

private fun <T : Any> StateKeeper<T, Unit>.withRestoration(owner: T, block: () -> Unit) {
    val state = prepare(owner, Unit)
    state.postProcess()
    block()
    state.restore()
}