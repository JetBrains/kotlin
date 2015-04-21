// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE

fun <T> foo(a: Array<T>): T = null!!

fun test(a: Array<in Int>) {
    foo(a) checkType { _<Any?>() }
}
