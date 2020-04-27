// INTENTION_TEXT: "Convert to 'forEachIndexed'"
// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>forEach { s ->
        println(s)
    }
}