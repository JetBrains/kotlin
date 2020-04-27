// INTENTION_TEXT: "Convert to 'scanIndexed'"
// WITH_RUNTIME
@OptIn(ExperimentalStdlibApi::class)
fun test(list: List<String>) {
    list.<caret>scan("") { acc, s ->
        acc + s
    }
}