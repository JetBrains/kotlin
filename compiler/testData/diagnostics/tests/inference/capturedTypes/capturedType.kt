// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// !CHECK_TYPE

fun <T> foo(array: Array<T>): Array<T> = array

fun test1(a1: Array<out Int>) {
    val b1: Array<out Int> = foo(a1)
    val c1 = foo(a1)
    c1 checkType { _<Array<out Int>>() }
}

fun test2(a2: Array<in Int>) {
    val b2: Array<in Int> = foo(a2)
    val c2 = foo(a2)
    c2 checkType { _<Array<in Int>>() }
}
