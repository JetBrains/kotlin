// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: ArgumentMatch(f : () -> ???, UNINFERRED_TYPE_IN_PARAMETER)

fun <T> foo(f: () -> T) {}


fun test() {
    foo { b }
}