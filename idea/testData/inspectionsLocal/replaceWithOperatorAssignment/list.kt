// WITH_RUNTIME
// HIGHLIGHT: INFORMATION

fun foo() {
    var list = listOf(1, 2, 3)
    // Should not be highlighted because it's the way we use to say explicitly
    // "yes, we want to re-assign this immutable list"
    list <caret>= list + 4
}