// WITH_RUNTIME
fun foo(f: () -> Unit) {}

class Bar {
    fun bar() {}
}

class Test {
    fun test() {
        Bar().run {
            foo { <caret>bar() }
        }
    }
}
