// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: ArgumentMatch(f : (Int) -> String, SUCCESS)

fun foo(f: (Int) -> String) {}


fun test() {
    foo { x -> "$x"}
}