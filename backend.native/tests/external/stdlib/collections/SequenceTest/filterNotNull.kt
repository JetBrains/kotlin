import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    val data = sequenceOf(null, "foo", null, "bar")
    val filtered = data.filterNotNull()
    assertEquals(listOf("foo", "bar"), filtered.toList())
}
