// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: emptyList() = ArgumentMatch(t : List<???>, SUCCESS)

class A {}

fun <T> foo(t: T) {}

fun <T> emptyList(): List<T> = throw Exception()

fun bar() {
    foo(emptyList())
}