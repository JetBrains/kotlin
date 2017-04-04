import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    for (sequence in listOf(emptySequence<String>(), sequenceOf<String>())) {
        assertEquals(0, sequence.map { true }.count())
    }
}
