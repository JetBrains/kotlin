// INTENTION_TEXT: "Convert to 'mapIndexedTo'"
// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>mapTo(mutableListOf()) { s ->
        s + s
    }
}