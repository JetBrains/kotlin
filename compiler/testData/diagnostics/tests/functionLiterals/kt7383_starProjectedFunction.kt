// KT-7383 Assertion failed when a star-projection of function type is used

fun foo() {
    val f : Function1<*, *> = { x -> x.toString() }
    f(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
}
