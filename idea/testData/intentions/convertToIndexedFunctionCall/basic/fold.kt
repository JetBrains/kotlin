// INTENTION_TEXT: "Convert to 'foldIndexed'"
// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>fold("") { acc, s ->
        acc + s
    }
}