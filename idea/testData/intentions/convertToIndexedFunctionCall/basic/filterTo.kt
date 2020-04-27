// INTENTION_TEXT: "Convert to 'filterIndexedTo'"
// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>filterTo(mutableListOf()) { s ->
        s == "a"
    }
}