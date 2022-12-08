// TARGET_BACKEND: JVM
// IGNORE_INLINER: IR
// WITH_STDLIB

package test

import kotlin.reflect.*
import kotlin.test.assertEquals

class Container<T>

class C<INV, in IN, out OUT> {
    fun getInv() = typeOf<Container<INV>>().arguments.single().type!!.classifier as KTypeParameter
    fun getIn() = typeOf<Container<IN>>().arguments.single().type!!.classifier as KTypeParameter
    fun getOut() = typeOf<Container<OUT>>().arguments.single().type!!.classifier as KTypeParameter
}

inline fun <reified X, Y : X> getY() = typeOf<Container<Y>>().arguments.single().type!!.classifier as KTypeParameter

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

    assertEquals("INV", c.getInv().toString())
    assertEquals("in IN", c.getIn().toString())
    assertEquals("out OUT", c.getOut().toString())
    assertEquals("X", x.toString())

    return "OK"
}
