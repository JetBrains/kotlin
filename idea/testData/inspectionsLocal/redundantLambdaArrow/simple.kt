fun foo(f: () -> Unit) {}

fun bar() {
    foo { <caret>-> }
}