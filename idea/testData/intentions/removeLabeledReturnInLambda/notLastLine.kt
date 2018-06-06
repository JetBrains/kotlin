// WITH_RUNTIME
// IS_APPLICABLE: FALSE

fun foo() {
    listOf(1,2,3).find {
        <caret>1
        return@find true
    }
}