// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: ArgumentMatch(f : (Int) -> String, SUCCESS)

fun <T> foo(f: (T) -> String) {}


fun test() {
    foo { (x: Int) -> "$x"}
}