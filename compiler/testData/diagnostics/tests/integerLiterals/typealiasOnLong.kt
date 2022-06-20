// FIR_IDENTICAL
// ISSUE: KT-52825

typealias LLL = Long

fun foo(a: Int, b: Int) {}
fun foo(a: LLL, b: LLL) {}

fun test() {
    foo(0, 0)
}
