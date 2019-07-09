// HIGHLIGHT: INFORMATION
fun main() {
    test("", <caret>object : FooBar {
        override fun foo() {
        }
    }, 1)
}

fun test(s: String, foo: Foo, i: Int) {}