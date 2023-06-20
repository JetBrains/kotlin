// FIR_IDENTICAL
// WITH_STDLIB
// FIR_DUMP

fun foo() {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.Int>")!>foo(1 to 2)<!>
    val x = foo(3 to 4)
}

fun <T : Comparable<T>> foo(vararg values: Pair<T, T>): List<T> = TODO()
