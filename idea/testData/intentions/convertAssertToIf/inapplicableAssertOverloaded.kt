// IS_APPLICABLE: false
// WITH_RUNTIME
fun foo() {
    <caret>assert(true, "")
}

fun assert(b: Boolean, s: String) {}