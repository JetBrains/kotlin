// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.full.isSubtypeOf
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class G<T>

fun number(): G<Number> = null!!
fun outNumber(): G<out Number> = null!!
fun inNumber(): G<in Number> = null!!
fun star(): G<*> = null!!

fun box(): String {
    val n = ::number.returnType
    val o = ::outNumber.returnType
    val i = ::inNumber.returnType
    val st = ::star.returnType

    // G<Number> <: G<out Number>
    assertTrue(n.isSubtypeOf(o))
    assertFalse(o.isSubtypeOf(n))

    // G<Number> <: G<in Number>
    assertTrue(n.isSubtypeOf(i))
    assertFalse(i.isSubtypeOf(n))

    // G<Number> <: G<*>
    assertTrue(n.isSubtypeOf(st))
    assertFalse(st.isSubtypeOf(n))

    // G<out Number> <: G<*>
    assertTrue(o.isSubtypeOf(st))
    assertFalse(st.isSubtypeOf(o))

    // G<in Number> <: G<*>
    assertTrue(i.isSubtypeOf(st))
    assertFalse(st.isSubtypeOf(i))

    return "OK"
}
