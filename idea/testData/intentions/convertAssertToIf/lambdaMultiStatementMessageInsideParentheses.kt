// WITH_RUNTIME
fun foo() {
    <caret>assert(true, {
        val value = 1
        "text and $value"
    })
}