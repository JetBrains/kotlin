// !CHECK_TYPE

package a

fun <T> id(t: T): T = t

fun <T> either(t1: T, <!UNUSED_PARAMETER!>t2<!>: T): T = t1

fun test() {
    val <!UNUSED_VARIABLE!>a<!>: Float = id(2.0.toFloat())

    val b = id(2.0)
    checkSubtype<Double>(b)

    val c = either<Number>(1, 2.3)
    checkSubtype<Number>(c)

    val d = either(11, 2.3)
    checkSubtype<Any>(d)

    val <!UNUSED_VARIABLE!>e<!>: Float = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>id(1)<!>
}