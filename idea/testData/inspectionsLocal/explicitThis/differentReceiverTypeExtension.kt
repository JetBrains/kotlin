// WITH_RUNTIME
// PROBLEM: none

class Foo {
    fun test() {
        Bar().apply {
            <caret>this@Foo.s()
        }
    }
}

class Bar


fun Foo.s() {}
fun Bar.s() {}