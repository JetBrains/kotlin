// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: 11 = ArgumentMatch(t : Comparable<out Any?>, SUCCESS)
// !ARG_2: ls = ArgumentMatch(l : List<Comparable<out Any?>>, SUCCESS)

fun <T> foo(t: T, l: List<T>) {}

fun use(vararg a: Any?) = a

fun test(a: Any, ls: List<String>) {
    use(foo(11, ls))
}