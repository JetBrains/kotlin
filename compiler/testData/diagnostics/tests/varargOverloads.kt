// FIR_IDENTICAL
// ISSUE: KT-62746

fun foo(bar: IntArray) {}

fun foo(vararg bar: Int) {}

fun main() {
    foo(1, 2)
    foo(intArrayOf(1, 2))
}
