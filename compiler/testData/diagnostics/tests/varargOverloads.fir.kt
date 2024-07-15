// ISSUE: KT-62746
// LANGUAGE: +ProhibitOverloadingBetweenVarargsAndArrays

<!CONFLICTING_OVERLOADS!>fun foo(bar: IntArray)<!> {}

<!CONFLICTING_OVERLOADS!>fun foo(vararg bar: Int)<!> {}

fun main() {
    foo(1, 2)
    foo(intArrayOf(1, 2))
}
