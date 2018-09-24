// WITH_RUNTIME
// PROBLEM: none

fun test() {
    Foo().apply {
        Foo().run {
            <caret>this@apply.isB = true
        }
    }
}