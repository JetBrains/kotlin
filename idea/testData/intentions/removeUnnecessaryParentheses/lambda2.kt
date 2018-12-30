// IS_APPLICABLE: false

fun main() {
    foo()
    <caret>({ foo() } as? () -> Unit)
}

fun foo() {}
