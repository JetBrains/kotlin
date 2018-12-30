// IS_APPLICABLE: false

fun main() {
    foo()
    <caret>({ foo() }.invoke())
}

fun foo() {}
