// INTENTION_TEXT: "Convert to 'reduceRightIndexedOrNull'"
// WITH_RUNTIME
@OptIn(ExperimentalStdlibApi::class)
fun test(list: List<String>) {
    list.<caret>reduceRightOrNull { s, acc ->
        s + acc
    }
}