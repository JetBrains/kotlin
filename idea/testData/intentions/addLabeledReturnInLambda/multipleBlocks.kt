// WITH_RUNTIME
// INTENTION_TEXT: "Add return@find"

fun foo() {
    listOf(1,2,3).find {
        if (it > 0) {
            <caret>true
        } else {
            false
        }
    }
}