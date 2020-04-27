// INTENTION_TEXT: "Convert to 'filterIndexed'"
// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>filter { s ->
        s == "a"
    }
}