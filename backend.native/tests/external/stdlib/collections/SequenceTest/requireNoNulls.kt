import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    val sequence = sequenceOf<String?>("foo", "bar")
    val notNull = sequence.requireNoNulls()
    assertEquals(listOf("foo", "bar"), notNull.toList())

    val sequenceWithNulls = sequenceOf("foo", null, "bar")
    val notNull2 = sequenceWithNulls.requireNoNulls() // shouldn't fail yet
    assertFails {
        // should throw an exception as we have a null
        notNull2.toList()
    }
}
