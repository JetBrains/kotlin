// WITH_RUNTIME
// HIGHLIGHT: GENERIC_ERROR_OR_WARNING

fun foo() {
    val list = mutableListOf(1, 2, 3)
    list <caret>= list + 4
}