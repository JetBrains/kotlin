// PROBLEM: none

fun foo(f: (String) -> Unit) {}
fun bar(f: String.() -> Unit) {}

fun test() {
    foo {
        <caret>bar {
        }
    }
}