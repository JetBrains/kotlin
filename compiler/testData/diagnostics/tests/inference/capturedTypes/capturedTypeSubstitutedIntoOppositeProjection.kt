// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE

fun <T> foo1(a1: Array<in T>, a2: Array<T>): T = null!!

fun test1(a1: Array<Int>, a2: Array<out Int>) {
    foo1(a1, a2) checkType { _<Int>() }
}

fun <T> foo2(a1: Array<T>, a2: Array<out T>): T = null!!

fun test2(a1: Array<in Int>, a2: Array<Int>) {
    foo2(a1, a2) checkType { _<Any?>() }
}