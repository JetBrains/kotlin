// INTENTION_TEXT: "Convert to 'forEachIndexed'"
// WITH_RUNTIME
fun test(list: List<String>, index: Int) {
    list.<caret>forEach { s ->
        println(s)
    }
}