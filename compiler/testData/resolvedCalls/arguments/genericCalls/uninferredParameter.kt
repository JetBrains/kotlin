// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: 11 = ArgumentMatch(t : ???, UNINFERRED_TYPE_IN_PARAMETER)
// !ARG_2: ls = ArgumentMatch(l : MutableList<???>, UNINFERRED_TYPE_IN_PARAMETER)

fun <T> foo(t: T, l: MutableList<T>) {}

fun use(vararg a: Any?) = a

fun test(ls: MutableList<String>) {
    use(foo(11, ls))
}