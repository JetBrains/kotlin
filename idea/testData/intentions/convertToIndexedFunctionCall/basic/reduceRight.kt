// INTENTION_TEXT: "Convert to 'reduceRightIndexed'"
// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>reduceRight { s, acc ->
        s + acc
    }
}