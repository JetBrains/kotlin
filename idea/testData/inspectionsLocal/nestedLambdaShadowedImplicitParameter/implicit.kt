fun foo(f: (String) -> Unit) {}

fun test() {
    foo {
        <caret>foo {
        }
    }
}