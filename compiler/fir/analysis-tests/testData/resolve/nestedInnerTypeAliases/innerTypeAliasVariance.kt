// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NestedTypeAliases

class P<T>

class C<out T>() {
    inner typealias TA = P<T>

    inner class Inner

    inner typealias TA1 = Inner

    fun test() {
        val a0: TA = TA() // OK, Should be represented as val a0: P<out Any> = P<Int>()
        val a1: TA = <!INITIALIZER_TYPE_MISMATCH(""), TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH!>P<Int>()<!> // OK
        val a2: TA1 = TA1() // OK
    }
}

fun test() {
    val a0: C<Any>.TA = C<Int>().TA() // OK, Should be represented as val a0: P<out Any> = P<Int>()
    val a1: C<Any>.TA = <!INITIALIZER_TYPE_MISMATCH("P<kotlin.Any>; P<kotlin.Int>"), TYPE_MISMATCH!>P<Int>()<!> // OK
    val a2: C<Any>.TA1 = C<Int>().<!UNRESOLVED_REFERENCE!>TA1<!>() // OK
}
