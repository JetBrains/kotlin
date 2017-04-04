import kotlin.test.*

data class Collector<out K, V>(val key: K, val values: MutableList<V> = mutableListOf<V>())

fun box() {

    fun <K> Collector<K, String>.accumulateIfEven(e: String) = apply { if (e.length % 2 == 0) values.add(e) }
    fun <K, V> Collector<K, V>.toPair() = key to values as List<V>

    val elements = listOf("foo", "bar", "flea", "zoo", "biscuit")
    val result = elements.groupingBy { it.first() }
            .fold({ k, e -> Collector<Char, String>(k) }, { k, acc, e -> acc.accumulateIfEven(e) })

    val ordered = result.values.sortedBy { it.key }.map { it.toPair() }
    assertEquals(listOf('b' to emptyList(), 'f' to listOf("flea"), 'z' to emptyList()), ordered)

    val moreElements = listOf("fire", "zero")
    val result2 = moreElements.groupingBy { it.first() }
            .foldTo(HashMap(result),
                    { k, e -> error("should not be called for $k") },
                    { k, acc, e -> acc.accumulateIfEven(e) })

    val ordered2 = result2.values.sortedBy { it.key }.map { it.toPair() }
    assertEquals(listOf('b' to emptyList(), 'f' to listOf("flea", "fire"), 'z' to listOf("zero")), ordered2)
}
