// "Add non-null asserted (!!) call" "true"

class A {
    fun foo() {}
}

fun A?.bar() {
    <caret>foo()
}