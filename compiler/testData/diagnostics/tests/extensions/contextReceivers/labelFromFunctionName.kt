// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers
// WITH_STDLIB

fun testLabels(source: Collection<String>) {
    val r = buildList {
        source.mapTo(this@buildList) { it.length }
    }
}