// !WITH_NEW_INFERENCE
// !CHECK_TYPE

package a

import checkSubtype

fun <T> id(t: T): T = t

fun <T> either(t1: T, t2: T): T = t1

fun test() {
    val a: Float = id(2.0.toFloat())

    val b = id(2.0)
    checkSubtype<Double>(b)

    val c = either<Number>(1, 2.3)
    checkSubtype<Number>(c)

    val d = either(11, 2.3)
    checkSubtype<Any>(d)

    val e: Float = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>id(1)<!>
}
