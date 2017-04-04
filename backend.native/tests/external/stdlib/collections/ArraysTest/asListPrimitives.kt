import kotlin.test.*

fun box() {
    // Array of primitives
    val arr = intArrayOf(1, 2, 3, 4, 2, 5)
    val list = arr.asList()
    assertEquals(list, arr.toList())

    assertTrue(2 in list)
    assertFalse(0 in list)
    assertTrue(list.containsAll(listOf(5, 4, 3)))
    assertFalse(list.containsAll(listOf(5, 6, 3)))

    expect(1) { list.indexOf(2) }
    expect(4) { list.lastIndexOf(2) }
    expect(-1) { list.indexOf(6) }

    assertEquals(list.subList(3, 5), listOf(4, 2))

    val iter = list.listIterator(2)
    expect(2) { iter.nextIndex() }
    expect(1) { iter.previousIndex() }
    expect(3) {
        iter.next()
        iter.previous()
        iter.next()
    }

    arr[2] = 4
    assertEquals(list, arr.toList())

    assertEquals(IntArray(0).asList(), emptyList<Int>())
}
