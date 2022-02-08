// TARGET_BACKEND: JVM

// WITH_STDLIB
// FULL_JDK

package collections

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.security.KeyPair


fun <T> testCollectionSize(c: Collection<T>) = assertEquals(0, c.size)
fun <T> testCollectionIsEmpty(c: Collection<T>) = assertTrue(c.isEmpty())
fun <T> testCollectionContains(c: Collection<T>) = assertTrue(c.contains(1 as Any?))
fun <T> testCollectionIterator(c: Collection<T>) {
    val it = c.iterator()
    while (it.hasNext()) {
        assertEquals(1, it.next() as Any?)
    }
}
fun <T> testCollectionContainsAll(c: Collection<T>) = assertTrue(c.containsAll(c))
fun <T> testMutableCollectionAdd(c: MutableCollection<T>, t: T) {
    c.add(t)
    assertEquals(1, c.size)
    assertTrue(c.contains(t))
}
fun <T> testMutableCollectionRemove(c: MutableCollection<T>, t: T) {
    c.remove(t)
    assertEquals(0, c.size)
    assertFalse(c.contains(t))
}
fun <T> testMutableCollectionIterator(c: MutableCollection<T>, t: T) {
    c.add(t)
    val it = c.iterator()
    while (it.hasNext()) {
        it.next()
        it.remove()
    }
    assertEquals(0, c.size)
}
fun <T> testMutableCollectionAddAll(c: MutableCollection<T>, t1: T, t2: T) {
    c.addAll(arrayListOf(t1, t2))
    assertEquals(arrayListOf(t1, t2), c)
}
fun <T> testMutableCollectionRemoveAll(c: MutableCollection<T>, t1: T, t2: T) {
    c.addAll(arrayListOf(t1, t2))
    c.removeAll(arrayListOf(t1))
    assertEquals(arrayListOf(t2), c)
}
fun <T> testMutableCollectionRetainAll(c: MutableCollection<T>, t1: T, t2: T) {
    c.addAll(arrayListOf(t1, t2))
    c.retainAll(arrayListOf(t1))
    assertEquals(arrayListOf(t1), c)
}
fun <T> testMutableCollectionClear(c: MutableCollection<T>) {
    c.clear()
    assertTrue(c.isEmpty())
}

fun testCollection() {
    testCollectionSize(arrayListOf<Int>())
    testCollectionIsEmpty(arrayListOf<String>())
    testCollectionContains(arrayListOf(1))
    testCollectionIterator(arrayListOf(1))
    testCollectionContainsAll(arrayListOf("a", "b"))

    testMutableCollectionAdd(arrayListOf(), "")
    testMutableCollectionRemove(arrayListOf("a"), "a")
    testMutableCollectionIterator(arrayListOf(), 1)
    testMutableCollectionAddAll(arrayListOf(), 1, 2)
    testMutableCollectionRemoveAll(arrayListOf(), 1, 2)
    testMutableCollectionRetainAll(arrayListOf(), 1, 2)
    testMutableCollectionClear(arrayListOf(1, 2))
}


fun <T> testListGet(l: List<T>, t: T) = assertEquals(t, l.get(0))
fun <T> testListIndexOf(l: List<T>, t: T) = assertEquals(0, l.indexOf(t))
fun <T> testListIterator(l: List<T>, t1 : T, t2 : T) {
    val indexes = arrayListOf<Int>()
    val result = arrayListOf<T>()
    val it = l.listIterator()
    while (it.hasNext()) {
        indexes.add(it.nextIndex())
        result.add(it.next())
    }
    while (it.hasPrevious()) {
        indexes.add(it.previousIndex())
        result.add(it.previous())
    }
    assertEquals(arrayListOf(0, 1, 1, 0), indexes)
    assertEquals(arrayListOf(t1, t2, t2, t1), result)
}
fun <T> testListSublist(l: List<T>, t: T) = assertEquals(arrayListOf(t), l.subList(0, 1))

fun <T> testMutableListSet(l: MutableList<T>, t: T) {
    l.set(0, t)
    assertEquals(arrayListOf(t), l)
}
fun <T> testMutableListIterator(l: MutableList<T>, t1: T, t2: T, t3: T) {
    val it = l.listIterator()
    while (it.hasNext()) {
        it.next()
        it.add(t3)
    }
    assertEquals(arrayListOf(t1, t3, t2, t3), l)
}

fun testList() {
    testListGet(arrayListOf(1), 1)
    testListIndexOf(arrayListOf(1), 1)
    testListIterator(arrayListOf("a", "b"), "a", "b")
    testListSublist(arrayListOf(1, 2), 1)

    testMutableListSet(arrayListOf(2), 4)
    testMutableListIterator(arrayListOf(1, 2), 1, 2, 3)
}

fun <K, V> testMapContainsKey(map: Map<K, V>, k: K) = assertTrue(map.containsKey(k))
fun <K, V> testMapKeys(map: Map<K, V>, k1: K, k2: K) = assertEqualCollections(hashSetOf(k1, k2), map.keys)
fun <K, V> testMapValues(map: Map<K, V>, v1: V, v2: V) = assertEqualCollections(hashSetOf(v1, v2), map.values)
fun <K, V> testMapEntrySet(map: Map<K, V>, k : K, v: V) {
    for (entry in map.entries) {
        assertEquals(k, entry.key)
        assertEquals(v, entry.value)
    }
}
fun <K, V> testMutableMapEntry(map: MutableMap<K, V>, k1 : K, v: V) {
    for (entry in map.entries) {
        entry.setValue(v)
    }
    assertEquals(hashMapOf(k1 to v), map)
}


fun testMap() {
    testMapContainsKey(hashMapOf(1 to 'a', 2 to 'b'), 2)
    testMapKeys(hashMapOf(1 to 'a', 2 to 'b'), 1, 2)
    testMapValues(hashMapOf(1 to 'a', 2 to 'b'), 'a', 'b')
    testMapEntrySet(hashMapOf(1 to 'a'), 1, 'a')
    testMutableMapEntry(hashMapOf(1 to 'a'), 1, 'b')
}

fun box() : String {
    testCollection()
    testList()
    testMap()
    return "OK"
}

fun <T> assertEqualCollections(c1: Collection<T>, c2: Collection<T>) = assertEquals(c1.toCollection(hashSetOf<T>()), c2.toCollection(hashSetOf<T>()))
