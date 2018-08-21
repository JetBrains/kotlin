fun foo(f: (String) -> Unit) {}
fun bar(s: String) {}

fun test() {
    foo {
        foo { s ->
            <caret>foo {
                bar(it)
            }
        }
    }
}