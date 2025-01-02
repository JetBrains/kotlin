/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.types.Variance
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val fooAttr = irAttribute<IrExpression, Int>(followAttributeOwner = true).create(null, "foo")
private var IrExpression.foo: Int? by fooAttr
private val barAttr = irAttribute<IrExpression, Boolean>(followAttributeOwner = true).create(null, "bar")
private var IrExpression.bar: Boolean? by barAttr
private val bazAttr = irAttribute<IrExpression, String>(followAttributeOwner = true).create(null, "baz")
private var IrExpression.baz: String? by bazAttr

class IrAttributeTests {
    private fun createIrElement(): IrExpression =
        IrConstImpl.constNull(0, 0, IrErrorTypeImpl(null, listOf(), Variance.INVARIANT))

    private val IrElement.allAttributes: Map<IrAttribute<*, *>, Any>
        get() = (this as IrElementBase).attributes


    @Test
    fun simpleSet() {
        val element = createIrElement()

        assertEquals(null, element.foo)
        assertTrue(element.attributes.isEmpty())

        element.foo = 10
        assertEquals(10, element.foo)
        assertEquals<Map<IrAttribute<*, *>, Any?>>(mapOf(fooAttr to 10), element.attributes)
    }

    @Test
    fun simpleChange() {
        val element = createIrElement()

        element.foo = 10
        element.foo = 200
        assertEquals(200, element.foo)
    }

    @Test
    fun simpleRemove() {
        val element = createIrElement()

        element.foo = 10
        element.foo = null
        assertEquals(null, element.foo)
    }

    @Test
    fun removeNotPresent() {
        val element = createIrElement()

        element.foo = null
        element.bar = true
        element.foo = null
        assertEquals(null, element.foo)
        assertEquals(true, element.bar)
    }

    @Test
    fun setAndRemoveMultiple() {
        val element = createIrElement()

        element.foo = 10
        element.bar = true
        element.baz = "test"
        assertEquals(10, element.foo)
        assertEquals(true, element.bar)
        assertEquals("test", element.baz)
        assertEquals<Map<IrAttribute<*, *>, Any?>>(
            mapOf(fooAttr to 10, barAttr to true, bazAttr to "test"),
            element.attributes
        )

        element.foo = 200
        element.bar = null
        assertEquals(200, element.foo)
        assertEquals(null, element.bar)
        assertEquals<Map<IrAttribute<*, *>, Any?>>(
            mapOf(fooAttr to 200, bazAttr to "test"),
            element.attributes
        )
    }

    @Test
    fun repeatedSetAndRemove() {
        val element = createIrElement()

        element.foo = -1
        element.bar = true
        repeat(3) {
            element.foo = it
            assertEquals(it, element.foo)
            assertEquals(true, element.bar)
            assertEquals(2, element.attributes.size)

            element.foo = null
        }
    }

    @Test
    fun copyToEmpty() {
        val element1 = createIrElement()
        element1.foo = 10
        element1.baz = "test"

        val element2 = createIrElement()
        element2.copyAttributes(element1)

        assertEquals(element1.attributes, element2.attributes)
    }

    @Test
    fun copyFromEmpty() {
        val element1 = createIrElement()

        val element2 = createIrElement()
        element2.foo = 10
        element2.baz = "test"
        element2.copyAttributes(element1)

        assertEquals(element2.foo, 10)
        assertEquals(element2.baz, "test")
    }

    @Test
    fun copyWithOverride() {
        val element1 = createIrElement()
        element1.foo = 10
        element1.baz = "new"

        val element2 = createIrElement()
        element2.baz = "prev"
        element2.bar = true
        element2.copyAttributes(element1)

        assertEquals(element2.foo, 10)
        assertEquals(element2.bar, true)
        assertEquals(element2.baz, "new")
    }

    @Test
    fun stressTest() {
        val element = createIrElement()
        val attributes = List(10) { irAttribute<IrExpression, Int>(followAttributeOwner = true).create(null, "attr$it") }
        val realAttributeValues = mutableMapOf<IrAttribute<IrExpression, Int>, Int?>()

        val rng = Random(1)
        repeat(1000) { i ->
            val attr = attributes.random(rng)
            when (rng.nextInt(6)) {
                0 -> {
                    element[attr] = i
                    realAttributeValues[attr] = i
                }
                1 -> {
                    element[attr] = null
                    realAttributeValues.remove(attr)
                }
                2 -> {
                    val srcEl = createIrElement()
                    srcEl[attr] = i
                    element.copyAttributes(srcEl)
                    realAttributeValues[attr] = i
                }
                3 -> {
                    assertEquals<Map<out IrAttribute<*, *>, Any?>>(element.attributes, realAttributeValues)
                }
                else -> {
                    assertEquals(realAttributeValues[attr], element[attr])
                }
            }
        }
    }
}