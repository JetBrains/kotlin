import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    val seq = sequenceOf(1 to 'a', 2 to 'b', 3 to 'c')
    val (ints, chars) = seq.unzip()
    assertEquals(listOf(1, 2, 3), ints)
    assertEquals(listOf('a', 'b', 'c'), chars)
}
