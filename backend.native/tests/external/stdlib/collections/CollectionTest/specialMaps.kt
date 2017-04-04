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

public fun <T> CompareContext<Iterator<T>>.iteratorBehavior() {
    propertyEquals { hasNext() }

    while (expected.hasNext()) {
        propertyEquals { next() }
        propertyEquals { hasNext() }
    }
    propertyFails { next() }
}

fun CompareContext<ListIterator<*>>.listIteratorProperties() {
    propertyEquals { hasNext() }
    propertyEquals { hasPrevious() }
    propertyEquals { nextIndex() }
    propertyEquals { previousIndex() }
}

fun <T> CompareContext<Set<T>>.setBehavior(objectName: String = "") {
    equalityBehavior(objectName)
    collectionBehavior(objectName)
    compareProperty({ iterator() }, { iteratorBehavior() })
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

public fun <K, V> CompareContext<Map<K, V>>.mapBehavior() {
    equalityBehavior()
    propertyEquals { size }
    propertyEquals { isEmpty() }

    (object {}).let { propertyEquals { containsKey(it as Any?) } }

    if (expected.isEmpty().not())
        propertyEquals { contains(keys.first()) }

    propertyEquals { containsKey(keys.firstOrNull()) }
    propertyEquals { containsValue(values.firstOrNull()) }
    propertyEquals { get(null as Any?) }

    compareProperty({ keys }, { setBehavior("keySet") })
    compareProperty({ entries }, { setBehavior("entrySet") })
    compareProperty({ values }, { collectionBehavior("values") })
}

fun box() {
    compare(hashMapOf<String, Int>(), mapOf<String, Int>()) { mapBehavior() }
    compare(linkedMapOf<Int, String>(), emptyMap<Int, String>()) { mapBehavior() }
    compare(linkedMapOf(2 to 3), mapOf(2 to 3)) { mapBehavior() }
}
