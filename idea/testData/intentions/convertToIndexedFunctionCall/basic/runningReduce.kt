// INTENTION_TEXT: "Convert to 'runningReduceIndexed'"
// WITH_RUNTIME
@OptIn(ExperimentalStdlibApi::class)
fun test(list: List<String>) {
    list.<caret>runningReduce { acc, s ->
        acc + s
    }
}