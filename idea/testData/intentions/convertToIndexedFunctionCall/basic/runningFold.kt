// INTENTION_TEXT: "Convert to 'runningFoldIndexed'"
// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>runningFold("") { acc, s ->
        acc + s
    }
}