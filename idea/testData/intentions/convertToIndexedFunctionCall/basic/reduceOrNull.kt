// INTENTION_TEXT: "Convert to 'reduceIndexedOrNull'"
// WITH_RUNTIME
@OptIn(ExperimentalStdlibApi::class)
fun test(list: List<String>) {
    list.<caret>reduceOrNull { acc, s ->
        acc + s
    }
}