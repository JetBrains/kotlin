// INTENTION_TEXT: "Convert to 'onEachIndexed'"
// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>onEach { s ->
        println(s)
    }
}