// WITH_RUNTIME
// INTENTION_TEXT: "Add return@find"

fun foo(): Int? {
    return listOf(1,2,3).find {
        <caret>true
    }
}