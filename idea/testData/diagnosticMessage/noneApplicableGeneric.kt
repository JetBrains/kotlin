// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: NONE_APPLICABLE
// !MESSAGE_TYPE: HTML

package p

fun <T> foo(t: T, a: Any, l: List<T>) = 1
fun <T, R> foo(t: T, r: R, lt: MutableList<T>, lr: MutableList<R>) = 2
fun <T> foo(t: T, a: Any, lt: MutableList<T>, lr: MutableList<T>) = 3

fun use(vararg a: Any?) = a

fun test(a: Any, li: MutableList<Int>, ls: MutableList<String>) {
    use(foo(11, a, li, ls))
}