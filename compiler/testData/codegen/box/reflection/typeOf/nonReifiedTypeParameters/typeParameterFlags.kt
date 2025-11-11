// WITH_REFLECT
// WITH_STDLIB
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_PHASE: 2.2.0
// ^^^ Expected <in IN>, actual <IN>.
//     at assertEquals

// FILE: lib.kt
package test

import kotlin.reflect.*

class Container<T>

class C<INV, in IN, out OUT> {
    fun getInv() = typeOf<Container<INV>>().arguments.single().type!!.classifier as KTypeParameter
    fun getIn() = typeOf<Container<IN>>().arguments.single().type!!.classifier as KTypeParameter
    fun getOut() = typeOf<Container<OUT>>().arguments.single().type!!.classifier as KTypeParameter
}

inline fun <reified X, Y : X> getY() = typeOf<Container<Y>>().arguments.single().type!!.classifier as KTypeParameter

// FILE: main.kt
package test
import kotlin.reflect.*
import kotlin.test.assertEquals

fun box(): String {
    val c = C<Any, Any, Any>()
    assertEquals(KVariance.INVARIANT, c.getInv().variance)
    assertEquals(KVariance.IN, c.getIn().variance)
    assertEquals(KVariance.OUT, c.getOut().variance)
    assertEquals(false, c.getInv().isReified)

    val y = getY<Any, Any>()
    assertEquals(false, y.isReified)
    val x = y.upperBounds.single().classifier as KTypeParameter
    assertEquals(true, x.isReified)
    assertEquals(KVariance.INVARIANT, x.variance)
    assertEquals("X", x.toString())

    assertEquals("INV", c.getInv().toString())
    assertEquals("in IN", c.getIn().toString())
    assertEquals("out OUT", c.getOut().toString())

    return "OK"
}
