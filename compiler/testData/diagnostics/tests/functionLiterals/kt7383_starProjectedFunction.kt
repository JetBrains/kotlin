// !WITH_NEW_INFERENCE
// KT-7383 Assertion failed when a star-projection of function type is used

fun foo() {
    val f : Function1<*, *> = { x -> x.toString() }
    <!MEMBER_PROJECTED_OUT{OI}!>f<!>(<!CONSTANT_EXPECTED_TYPE_MISMATCH{NI}!>1<!>)
}
