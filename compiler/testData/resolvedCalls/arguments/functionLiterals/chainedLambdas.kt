// !CALL: foo

fun <T, S, R> foo(t: T, f: (T) -> S, g: (S) -> R) {}


fun test() {
    foo(1, { x -> "$x"}, { y -> y.length })
}