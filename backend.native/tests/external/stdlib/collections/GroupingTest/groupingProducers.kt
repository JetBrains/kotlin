import kotlin.test.*


fun box() {

    fun <T, K> verifyGrouping(grouping: Grouping<T, K>, expectedElements: List<T>, expectedKeys: List<K>) {
        val elements = grouping.sourceIterator().asSequence().toList()
        val keys = elements.map { grouping.keyOf(it) } // TODO: replace with grouping::keyOf when supported in JS

        assertEquals(expectedElements, elements)
        assertEquals(expectedKeys, keys)
    }

    val elements = listOf("foo", "bar", "value", "x")
    val keySelector: (String) -> Int = { it.length }
    val keys = elements.map(keySelector)

    fun verifyGrouping(grouping: Grouping<String, Int>) = verifyGrouping(grouping, elements, keys)

    verifyGrouping(elements.groupingBy { it.length })
    verifyGrouping(elements.toTypedArray().groupingBy(keySelector))
    verifyGrouping(elements.asSequence().groupingBy(keySelector))

    val charSeq = "some sequence of chars"
    verifyGrouping(charSeq.groupingBy { it.toInt() }, charSeq.toList(), charSeq.map { it.toInt() })
}
