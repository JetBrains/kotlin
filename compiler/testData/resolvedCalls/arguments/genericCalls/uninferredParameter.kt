fun <T> foo(t: T, l: MutableList<T>) {}

fun use(vararg a: Any?) = a

fun test(ls: MutableList<String>) {
    use(<caret>foo(11, ls))
}