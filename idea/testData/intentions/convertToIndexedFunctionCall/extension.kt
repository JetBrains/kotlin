// INTENTION_TEXT: "Convert to 'forEachIndexed'"
// WITH_RUNTIME
fun List<String>.test() {
    <caret>forEach { s ->
        println(s)
    }
}