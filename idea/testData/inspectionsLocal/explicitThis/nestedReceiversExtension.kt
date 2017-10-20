// WITH_RUNTIME
// PROBLEM: none

class Foo {
    fun test() {
        Bar().apply {
            "".run {
                <caret>this@apply.s()
            }
        }
    }
}

class Bar

fun Foo.s() {}
fun Bar.s() {}