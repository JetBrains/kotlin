// !CALL: foo

fun foo() {}

fun test() {
    foo { x -> "$x"}
}