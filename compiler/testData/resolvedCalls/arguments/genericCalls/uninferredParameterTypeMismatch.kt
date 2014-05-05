// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: 11 = ArgumentMatch(l : List<???>, TYPE_MISMATCH)

fun <T> foo(l: List<T>) {}

fun test() {
    foo(11)
}