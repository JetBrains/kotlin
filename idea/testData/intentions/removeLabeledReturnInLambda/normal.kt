// WITH_RUNTIME
// INTENTION_TEXT: "Remove return@find"

fun foo() {
    listOf(1,2,3).find {
        return@find <caret>true
    }
}