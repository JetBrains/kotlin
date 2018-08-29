// FIX: Add explicit parameter name to outer lambda

fun foo(f: (String) -> Unit) {}
fun bar(s: String) {}

fun test() {
    foo {
        foo { s ->
            foo {
                bar(it<caret>)
            }
        }
    }
}