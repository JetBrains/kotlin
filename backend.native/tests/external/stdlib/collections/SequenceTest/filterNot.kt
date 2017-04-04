import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    val data = sequenceOf(null, "foo", null, "bar")
    val filtered = data.filterNot { it == null }
    assertEquals(listOf("foo", "bar"), filtered.toList())
}
