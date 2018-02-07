package test

fun foo() {
    fun <caret>innerGoo() {
        foo()
    }
    innerGoo()
}