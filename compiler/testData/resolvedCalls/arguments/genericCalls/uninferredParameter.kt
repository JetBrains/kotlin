// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: 11 = ArgumentMatch(t : ???, SUCCESS)
// !ARG_2: ls = ArgumentMatch(l : MutableList<???>, SUCCESS)

fun <T> foo(t: T, l: MutableList<T>) {}

fun use(vararg a: Any?) = a

fun test(ls: MutableList<String>) {
    use(foo(11, ls))
}