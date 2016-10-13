// IS_APPLICABLE: false

fun foo() {
    <caret>if (true) {
        if (false) {
            foo()
        }
    } else {
    }
}