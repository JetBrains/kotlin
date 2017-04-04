import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    val data = sequenceOf("foo", "bar")
    val indexed = data.withIndex().map { it.value.substring(0..it.index) }.toList()
    assertEquals(listOf("f", "ba"), indexed)
}
