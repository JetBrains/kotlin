// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// !CHECK_TYPE

fun <T> foo(a: Array<T>): Array<Array<T>> = throw Exception()

fun test1(a1: Array<out Int>) {
    val b1: Array<out Array<out Int>> = foo(a1)
    val c1 = foo(a1)
    c1 checkType { _<Array<out Array<out Int>>>() }
}

fun test2(a2: Array<in Int>) {
    val b2: Array<out Array<in Int>> = foo(a2)
    val c2 = foo(a2)
    c2 checkType { _<Array<out Array<in Int>>>() }
}