import kotlin.test.*

public fun <T> compare(expected: T, actual: T, block:CompareContext<T>.() -> Unit) {
    CompareContext(expected, actual).block()
}

public class CompareContext<out T>(public val expected: T, public val actual: T) {

    public fun equals(message: String = "") {
        assertEquals(expected, actual, message)
    }
    public fun <P> propertyEquals(message: String = "", getter: T.() -> P) {
        assertEquals(expected.getter(), actual.getter(), message)
    }
    public fun propertyFails(getter: T.() -> Unit) { assertFailEquals({expected.getter()}, {actual.getter()}) }
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

public fun <T> CompareContext<Iterator<T>>.iteratorBehavior() {
    propertyEquals { hasNext() }

    while (expected.hasNext()) {
        propertyEquals { next() }
        propertyEquals { hasNext() }
    }
    propertyFails { next() }
}

fun box() {
    fun <T, E> checkContract(array: T, toList: T.() -> List<E>, iterator: T.() -> Iterator<E>) =
            compare(array.toList().iterator(), array.iterator()) {
                iteratorBehavior()
            }

    checkContract(arrayOf("a", "b", "c"), { toList() }, { iterator() })
    checkContract(intArrayOf(), { toList() }, { iterator() })
    checkContract(intArrayOf(1, 2, 3), { toList() }, { iterator() })
    checkContract(shortArrayOf(1, 2, 3), { toList() }, { iterator() })
    checkContract(byteArrayOf(1, 2, 3), { toList() }, { iterator() })
    checkContract(longArrayOf(1L, 2L, 3L), { toList() }, { iterator() })
    checkContract(doubleArrayOf(2.0, 3.0, 9.0), { toList() }, { iterator() })
    checkContract(floatArrayOf(2f, 3f, 9f), { toList() }, { iterator() })
    checkContract(charArrayOf('a', 'b', 'c'), { toList() }, { iterator() })
    checkContract(booleanArrayOf(true, false), { toList() }, { iterator() })
}