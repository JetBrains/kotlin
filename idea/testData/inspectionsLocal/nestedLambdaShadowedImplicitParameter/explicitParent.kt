// PROBLEM: none

fun foo(f: (String) -> Unit) {}

fun test() {
    foo { s ->
        <caret>foo {
        }
    }
}