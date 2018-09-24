// PROBLEM: none

fun foo(f: (String) -> Unit) {}
fun bar(f: String.() -> Unit) {}
fun baz(s: String) {}

fun test() {
    foo {
        <caret>bar {
            baz(it<caret>)
        }
    }
}