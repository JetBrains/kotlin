// INTENTION_TEXT: "Convert to 'mapIndexed'"
// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>map { s ->
        s + s
    }
}