import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    val result = sequenceOf<Int>().flatMap { (0..it).asSequence() }
    assertTrue(result.none())
}
