// !CALL: foo

fun foo(f: (Int) -> String) {}

fun test() {
    foo { x -> "$x"}
}