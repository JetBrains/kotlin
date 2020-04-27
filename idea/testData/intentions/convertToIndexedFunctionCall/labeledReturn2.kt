// INTENTION_TEXT: "Convert to 'mapIndexed'"
// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>map { s ->
        when (s) {
            "a" -> return@map 1
            "b" -> return@map 2
            else -> return@map 3
        }
    }
}