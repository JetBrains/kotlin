// WITH_RUNTIME
// INTENTION_TEXT: "Add return@label"

fun foo() {
    listOf(1,2,3).find label@{
        <caret>true
    }
}