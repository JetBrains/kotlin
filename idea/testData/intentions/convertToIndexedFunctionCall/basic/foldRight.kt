// INTENTION_TEXT: "Convert to 'foldRightIndexed'"
// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>foldRight("") { s, acc ->
        s + acc
    }
}