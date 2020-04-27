// INTENTION_TEXT: "Convert to 'mapIndexedNotNull'"
// WITH_RUNTIME
fun test(list: List<String?>) {
    list.<caret>mapNotNull { s ->
        s
    }
}