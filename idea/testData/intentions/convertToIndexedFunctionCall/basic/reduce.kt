// INTENTION_TEXT: "Convert to 'reduceIndexed'"
// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>reduce { acc, s ->
        acc + s
    }
}