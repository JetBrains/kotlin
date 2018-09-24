// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

fun <T> foo(a1: Array<T>, a2: Array<out T>): T = null!!

fun test(a1: Array<in Int>, a2: Array<Int>) {

    val c: Int = <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>foo(a1, a2)<!>

}
