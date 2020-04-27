// INTENTION_TEXT: "Convert to 'mapIndexedNotNullTo'"
// WITH_RUNTIME
fun test(list: List<String?>) {
    list.<caret>mapNotNullTo(mutableListOf()) { s ->
        s
    }
}