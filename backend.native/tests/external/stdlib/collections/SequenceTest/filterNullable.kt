import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    val data = sequenceOf(null, "foo", null, "bar")
    val filtered = data.filter { it == null || it == "foo" }
    assertEquals(listOf(null, "foo", null), filtered.toList())
}
