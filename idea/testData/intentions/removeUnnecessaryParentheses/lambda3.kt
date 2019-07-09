fun main() {
    foo()
    <caret>({ foo() }.invoke())
}

fun foo() {}
