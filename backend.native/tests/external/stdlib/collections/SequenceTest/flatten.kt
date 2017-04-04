import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    val expected = listOf(0, 1, 0, 1, 2)

    val seq = sequenceOf((0..1).asSequence(), (0..2).asSequence()).flatten()
    assertEquals(expected, seq.toList())

    val seqMappedSeq = sequenceOf(1, 2).map { (0..it).asSequence() }.flatten()
    assertEquals(expected, seqMappedSeq.toList())

    val seqOfIterable = sequenceOf(0..1, 0..2).flatten()
    assertEquals(expected, seqOfIterable.toList())

    val seqMappedIterable = sequenceOf(1, 2).map { 0..it }.flatten()
    assertEquals(expected, seqMappedIterable.toList())
}
