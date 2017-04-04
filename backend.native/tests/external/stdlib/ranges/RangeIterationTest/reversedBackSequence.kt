import kotlin.test.*

fun <T> compare(expected: T, actual: T, block: CompareContext<T>.() -> Unit) {
    CompareContext(expected, actual).block()
}

class CompareContext<out T>(public val expected: T, public val actual: T) {

    public fun equals(message: String = "") {
        assertEquals(expected, actual, message)
    }

    public fun <P> propertyEquals(message: String = "", getter: T.() -> P) {
        assertEquals(expected.getter(), actual.getter(), message)
    }

    public fun propertyFails(getter: T.() -> Unit) {
        assertFailEquals({ expected.getter() }, { actual.getter() })
    }

    public fun <P> compareProperty(getter: T.() -> P, block: CompareContext<P>.() -> Unit) {
        compare(expected.getter(), actual.getter(), block)
    }

    private fun assertFailEquals(expected: () -> Unit, actual: () -> Unit) {
        val expectedFail = assertFails(expected)
        val actualFail = assertFails(actual)
        //assertEquals(expectedFail != null, actualFail != null)
        assertTypeEquals(expectedFail, actualFail)
    }
}

fun <T> CompareContext<Iterator<T>>.iteratorBehavior() {
    propertyEquals { hasNext() }

    while (expected.hasNext()) {
        propertyEquals { next() }
        propertyEquals { hasNext() }
    }
    propertyFails { next() }
}

public fun <N : Any> doTest(
        sequence: Iterable<N>,
        expectedFirst: N,
        expectedLast: N,
        expectedIncrement: Number,
        expectedElements: List<N>
) {
    val first: Any
    val last: Any
    val increment: Number
    when (sequence) {
        is IntProgression -> {
            first = sequence.first
            last = sequence.last
            increment = sequence.step
        }
        is LongProgression -> {
            first = sequence.first
            last = sequence.last
            increment = sequence.step
        }
        is CharProgression -> {
            first = sequence.first
            last = sequence.last
            increment = sequence.step
        }
        else -> throw IllegalArgumentException("Unsupported sequence type: $sequence")
    }

    assertEquals(expectedFirst, first)
    assertEquals(expectedLast, last)
    assertEquals(expectedIncrement, increment)

    if (expectedElements.isEmpty())
        assertTrue(sequence.none())
    else
        assertEquals(expectedElements, sequence.toList())

    compare(expectedElements.iterator(), sequence.iterator()) {
        iteratorBehavior()
    }
}

fun box() {
    doTest((5 downTo 3).reversed(), 3, 5, 1, listOf(3, 4, 5))
    doTest((5.toByte() downTo 3.toByte()).reversed(), 3, 5, 1, listOf(3, 4, 5))
    doTest((5.toShort() downTo 3.toShort()).reversed(), 3, 5, 1, listOf(3, 4, 5))
    doTest((5.toLong() downTo 3.toLong()).reversed(), 3.toLong(), 5.toLong(), 1.toLong(), listOf<Long>(3, 4, 5))

    doTest(('c' downTo 'a').reversed(), 'a', 'c', 1, listOf('a', 'b', 'c'))

}

fun reversedSimpleSteppedRange() {
    doTest((3..9 step 2).reversed(), 9, 3, -2, listOf(9, 7, 5, 3))
    doTest((3.toByte()..9.toByte() step 2).reversed(), 9, 3, -2, listOf(9, 7, 5, 3))
    doTest((3.toShort()..9.toShort() step 2).reversed(), 9, 3, -2, listOf(9, 7, 5, 3))
    doTest((3.toLong()..9.toLong() step 2.toLong()).reversed(), 9.toLong(), 3.toLong(), -2.toLong(), listOf<Long>(9, 7, 5, 3))

    doTest(('c'..'g' step 2).reversed(), 'g', 'c', -2, listOf('g', 'e', 'c'))
}
