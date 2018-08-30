// FIX: Replace 'it' with explicit parameter

fun foo(f: (String) -> Unit) {}
fun bar(s: String) {}

fun test() {
    foo {
        foo {
            bar(it)
            bar(it)
            bar(it<caret>)
        }
    }
}