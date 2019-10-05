// IS_APPLICABLE: false

fun foo() {
    if (true) {

    } <caret>else {
        if (false) {
            foo()
        } else {

        }
    }
}