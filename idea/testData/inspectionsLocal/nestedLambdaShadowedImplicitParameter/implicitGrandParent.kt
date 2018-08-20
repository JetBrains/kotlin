fun foo(f: (String) -> Unit) {}

fun test() {
    foo {
        foo { s ->
            <caret>foo {
            }
        }
    }
}