// FIX: Rename 'it'

fun foo(f: (String) -> Unit) {}
fun bar(s: String) {}

fun test() {
    foo {
        foo {
            bar(it<caret>)
        }
    }
}