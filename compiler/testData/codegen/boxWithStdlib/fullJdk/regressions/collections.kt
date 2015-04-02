package collections

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.security.KeyPair


fun <T> testCollectionSize(c: Collection<T>) = assertEquals(0, c.size())
fun <T> testCollectionIsEmpty(c: Collection<T>) = assertTrue(c.isEmpty())
fun <T> testCollectionContains(c: Collection<T>) = assertTrue(c.contains(1))
fun <T> testCollectionIterator(c: Collection<T>) {
    val it = c.iterator()
    while (it.hasNext()) {
        assertEquals(1, it.next())
    }
}
fun <T> testCollectionContainsAll(c: Collection<T>) = assertTrue(c.containsAll(c))
fun <T> testMutableCollectionAdd(c: MutableCollection<T>, t: T) {
    c.add(t)
    assertEquals(1, c.size())
    assertTrue(c.contains(t))
}
fun <T> testMutableCollectionRemove(c: MutableCollection<T>, t: T) {
    c.remove(t)
    assertEquals(0, c.size())
    assertFalse(c.contains(t))
}
fun <T> testMutableCollectionIterator(c: MutableCollection<T>, t: T) {
    c.add(t)
    val it = c.iterator()
    while (it.hasNext()) {
        it.next()
        it.remove()
    }
    assertEquals(0, c.size())
}
fun <T> testMutableCollectionAddAll(c: MutableCollection<T>, t1: T, t2: T) {
    c.addAll(arrayList(t1, t2))
    assertEquals(arrayList(t1, t2), c)
}
fun <T> testMutableCollectionRemoveAll(c: MutableCollection<T>, t1: T, t2: T) {
    c.addAll(arrayList(t1, t2))
    c.removeAll(arrayList(t1))
    assertEquals(arrayList(t2), c)
}
fun <T> testMutableCollectionRetainAll(c: MutableCollection<T>, t1: T, t2: T) {
    c.addAll(arrayList(t1, t2))
    c.retainAll(arrayList(t1))
    assertEquals(arrayList(t1), c)
}
fun <T> testMutableCollectionClear(c: MutableCollection<T>) {
    c.clear()
    assertTrue(c.isEmpty())
}

fun testCollection() {
    testCollectionSize(arrayList<Int>())
    testCollectionIsEmpty(arrayList<String>())
    testCollectionContains(arrayList(1))
    testCollectionIterator(arrayList(1))
    testCollectionContainsAll(arrayList("a", "b"))

    testMutableCollectionAdd(arrayList(), "")
    testMutableCollectionRemove(arrayList("a"), "a")
    testMutableCollectionIterator(arrayList(), 1)
    testMutableCollectionAddAll(arrayList(), 1, 2)
    testMutableCollectionRemoveAll(arrayList(), 1, 2)
    testMutableCollectionRetainAll(arrayList(), 1, 2)
    testMutableCollectionClear(arrayList(1, 2))
}


fun <T> testListGet(l: List<T>, t: T) = assertEquals(t, l.get(0))
fun <T> testListIndexOf(l: List<T>, t: T) = assertEquals(0, l.indexOf(t))
fun <T> testListIterator(l: List<T>, t1 : T, t2 : T) {
    val indexes = arrayList<Int>()
    val result = arrayList<T>()
    val it = l.listIterator()
    while (it.hasNext()) {
        indexes.add(it.nextIndex())
        result.add(it.next())
    }
    while (it.hasPrevious()) {
        indexes.add(it.previousIndex())
        result.add(it.previous())
    }
    assertEquals(arrayList(0, 1, 1, 0), indexes)
    assertEquals(arrayList(t1, t2, t2, t1), result)
}
fun <T> testListSublist(l: List<T>, t: T) = assertEquals(arrayList(t), l.subList(0, 1))

fun <T> testMutableListSet(l: MutableList<T>, t: T) {
    l.set(0, t)
    assertEquals(arrayList(t), l)
}
fun <T> testMutableListIterator(l: MutableList<T>, t1: T, t2: T, t3: T) {
    val it = l.listIterator()
    while (it.hasNext()) {
        it.next()
        it.add(t3)
    }
    assertEquals(arrayList(t1, t3, t2, t3), l)
}

fun testList() {
    testListGet(arrayList(1), 1)
    testListIndexOf(arrayList(1), 1)
    testListIterator(arrayList("a", "b"), "a", "b")
    testListSublist(arrayList(1, 2), 1)

    testMutableListSet(arrayList(2), 4)
    testMutableListIterator(arrayList(1, 2), 1, 2, 3)
}

fun <K, V> testMapContainsKey(map: Map<K, V>, k: K) = assertTrue(map.containsKey(k))
fun <K, V> testMapKeys(map: Map<K, V>, k1: K, k2: K) = assertEqualCollections(hashSet(k1, k2), map.keySet())
fun <K, V> testMapValues(map: Map<K, V>, v1: V, v2: V) = assertEqualCollections(hashSet(v1, v2), map.values())
fun <K, V> testMapEntrySet(map: Map<K, V>, k : K, v: V) {
    for (entry in map.entrySet()) {
        assertEquals(k, entry.key)
        assertEquals(v, entry.value)
    }
}
fun <K, V> testMutableMapEntry(map: MutableMap<K, V>, k1 : K, v: V) {
    for (entry in map.entrySet()) {
        entry.setValue(v)
    }
    assertEquals(hashMap(k1 to v), map)
}


fun testMap() {
    testMapContainsKey(hashMap(1 to 'a', 2 to 'b'), 2)
    testMapKeys(hashMap(1 to 'a', 2 to 'b'), 1, 2)
    testMapValues(hashMap(1 to 'a', 2 to 'b'), 'a', 'b')
    testMapEntrySet(hashMap(1 to 'a'), 1, 'a')
    testMutableMapEntry(hashMap(1 to 'a'), 1, 'b')
}

fun box() : String {
    testCollection()
    testList()
    testMap()
    return "OK"
}

fun <T> assertEqualCollections(c1: Collection<T>, c2: Collection<T>) = assertEquals(c1.toCollection(hashSet<T>()), c2.toCollection(hashSet<T>()))
