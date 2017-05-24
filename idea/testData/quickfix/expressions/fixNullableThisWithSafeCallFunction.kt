// "Replace with safe (this?.) call" "true"

class A {
    fun foo() {}
}

fun A?.bar() {
    <caret>foo()
}