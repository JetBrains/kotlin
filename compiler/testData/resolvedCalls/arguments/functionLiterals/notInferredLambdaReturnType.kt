// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: ArgumentMatch(f : () -> ???, SUCCESS)

fun <T> foo(f: () -> T) {}


fun test() {
    foo { b }
}