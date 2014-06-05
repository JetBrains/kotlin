fun <T> foo(t: T, l: List<T>) {}

fun use(vararg a: Any?) = a

fun test(a: Any, ls: List<String>) {
    use(<caret>foo(11, ls))
}