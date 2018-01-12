// PROBLEM: none
// WITH_RUNTIME
class Foo

class Test {
    fun foo(): Any = ""

    fun bar() {}

    fun test(a: Any) {
        foo().apply {
            <caret>if (this is Foo) {
                bar()
            }
        }
    }
}
