// WITH_RUNTIME
// INTENTION_TEXT: "Remove return@label"

fun foo() {
    listOf(1,2,3).find label@{
        return@label <caret>true
    }
}