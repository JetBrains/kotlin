import kotlin.test.*

fun box() {
    val arr = arrayOf("a", "b", "c", "d", "b", "e")
    val list = arr.asList()

    assertEquals(list, arr.toList())

    assertTrue("b" in list)
    assertFalse("z" in list)

    expect(1) { list.indexOf("b") }
    expect(4) { list.lastIndexOf("b") }
    expect(-1) { list.indexOf("x") }

    assertTrue(list.containsAll(listOf("e", "d", "c")))
    assertFalse(list.containsAll(listOf("e", "x", "c")))

    assertEquals(list.subList(3, 5), listOf("d", "b"))

    val iter = list.listIterator(2)
    expect(2) { iter.nextIndex() }
    expect(1) { iter.previousIndex() }
    expect("c") {
        iter.next()
        iter.previous()
        iter.next()
    }

    arr[2] = "xx"
    assertEquals(list, arr.toList())

    assertEquals(Array(0, { "" }).asList(), emptyList<String>())
}
