import kotlin.test.*
import kotlin.comparisons.*

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

fun <T> CompareContext<T>.equalityBehavior(objectName: String = "") {
    val prefix = objectName + if (objectName.isNotEmpty()) "." else ""
    equals(objectName)
    propertyEquals(prefix + "hashCode") { hashCode() }
    propertyEquals(prefix + "toString") { toString() }
}

fun CompareContext<ListIterator<*>>.listIteratorProperties() {
    propertyEquals { hasNext() }
    propertyEquals { hasPrevious() }
    propertyEquals { nextIndex() }
    propertyEquals { previousIndex() }
}

fun <T> CompareContext<Iterator<T>>.iteratorBehavior() {
    propertyEquals { hasNext() }

    while (expected.hasNext()) {
        propertyEquals { next() }
        propertyEquals { hasNext() }
    }
    propertyFails { next() }
}

fun <T> CompareContext<Collection<T>>.collectionBehavior(objectName: String = "") {
    val prefix = objectName + if (objectName.isNotEmpty()) "." else ""
    propertyEquals(prefix + "size") { size }
    propertyEquals(prefix + "isEmpty") { isEmpty() }

    (object {}).let { propertyEquals { contains(it as Any?) } }
    propertyEquals { contains(firstOrNull()) }
    propertyEquals { containsAll(this) }
}

fun <T> CompareContext<ListIterator<T>>.listIteratorBehavior() {
    listIteratorProperties()

    while (expected.hasNext()) {
        propertyEquals { next() }
        listIteratorProperties()
    }
    propertyFails { next() }

    while (expected.hasPrevious()) {
        propertyEquals { previous() }
        listIteratorProperties()
    }
    propertyFails { previous() }
}

fun <T> CompareContext<Set<T>>.setBehavior(objectName: String = "") {
    equalityBehavior(objectName)
    collectionBehavior(objectName)
    compareProperty({ iterator() }, { iteratorBehavior() })
}

fun <T> CompareContext<List<T>>.listBehavior() {
    equalityBehavior()
    collectionBehavior()
    compareProperty({ listIterator() }, { listIteratorBehavior() })
    compareProperty({ listIterator(0) }, { listIteratorBehavior() })

    propertyFails { listIterator(-1) }
    propertyFails { listIterator(size + 1) }

    for (index in expected.indices)
        propertyEquals { this[index] }

    propertyFails { this[size] }

    propertyEquals { indexOf(elementAtOrNull(0)) }
    propertyEquals { lastIndexOf(elementAtOrNull(0)) }

    propertyFails { subList(0, size + 1) }
    propertyFails { subList(-1, 0) }
    propertyEquals { subList(0, size) }
}

fun box() {
    compare(linkedSetOf<Int>(), setOf<Int>()) { setBehavior() }
    compare(hashSetOf<Double>(), emptySet<Double>()) { setBehavior() }
    compare(listOf("value").toMutableSet(), setOf("value")) { setBehavior() }
}
