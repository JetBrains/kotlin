// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: emptyList() = ArgumentMatch(t : MutableList<???>, UNINFERRED_TYPE_IN_PARAMETER)

class A {}

fun <T> foo(t: T) {}

fun <T> emptyList(): MutableList<T> = throw Exception()

fun bar() {
    foo(emptyList())
}