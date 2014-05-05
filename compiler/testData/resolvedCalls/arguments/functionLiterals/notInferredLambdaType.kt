// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: ArgumentMatch(f : (???) -> String, UNINFERRED_TYPE_IN_PARAMETER)

fun <T> foo(f: (T) -> String) {}


fun test() {
    foo { x -> "$x"}
}