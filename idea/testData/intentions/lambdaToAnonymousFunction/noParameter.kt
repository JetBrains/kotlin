fun foo(f: () -> String) {}

fun test() {
    foo { <caret>-> "" }
}