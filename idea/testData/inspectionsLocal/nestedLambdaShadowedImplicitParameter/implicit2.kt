fun foo(f: (String) -> Unit) {}
fun bar(f: (Int) -> Unit) {}

fun test() {
    foo {
        val s: String = it
        <caret>bar {
            val i: Int = it
        }
    }
}