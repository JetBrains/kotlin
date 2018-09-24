// WITH_RUNTIME
// PROBLEM: none

fun test() {
    Foo().apply {
        Bar().run {
            <caret>this@apply.isB = true
        }
    }
}