// INTENTION_TEXT: "Convert to 'scanReduceIndexed'"
// WITH_RUNTIME
// DISABLE-ERRORS
@OptIn(ExperimentalStdlibApi::class)
fun test(list: List<String>) {
    list.<caret>scanReduce { acc, s ->
        acc + s
    }
}