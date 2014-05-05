// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: ArgumentUnmapped

fun foo() {}

fun test() {
    foo { x -> "$x"}
}