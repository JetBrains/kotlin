import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    val comparator = compareBy { s: String -> s.reversed() }
    assertEquals(listOf("act", "wast", "test"), sequenceOf("act", "test", "wast").sortedWith(comparator).toList())
}
