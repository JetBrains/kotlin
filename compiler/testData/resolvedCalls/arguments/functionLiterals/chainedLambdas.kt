// !ONLY_ARGUMENTS
// !CALL: foo
// !ARG_1: ArgumentMatch(t : Int, SUCCESS)
// !ARG_2: ArgumentMatch(f : (Int) -> String, SUCCESS)
// !ARG_3: ArgumentMatch(g : (String) -> Int, SUCCESS)

fun <T, S, R> foo(t: T, f: (T) -> S, g: (S) -> R) {}


fun test() {
    foo(1, { x -> "$x"}, { y -> y.length })
}