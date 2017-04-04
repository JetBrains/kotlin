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
    doTest((3..5).reversed(), 5, 3, -1, listOf(5, 4, 3))
    doTest((3.toByte()..5.toByte()).reversed(), 5, 3, -1, listOf(5, 4, 3))
    doTest((3.toShort()..5.toShort()).reversed(), 5, 3, -1, listOf(5, 4, 3))
    doTest((3.toLong()..5.toLong()).reversed(), 5.toLong(), 3.toLong(), -1.toLong(), listOf<Long>(5, 4, 3))

    doTest(('a'..'c').reversed(), 'c', 'a', -1, listOf('c', 'b', 'a'))
}
