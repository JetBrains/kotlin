package runtime.collections.array_list2

import kotlin.test.*

fun testIteratorNext() {
    val a = arrayListOf("1", "2", "3", "4", "5")
    val it = a.listIterator()
    assertFailsWith<NoSuchElementException> {
        while (true) {
            it.next()
        }
    }
}

fun testIteratorPrevious() {
    val a = arrayListOf("1", "2", "3", "4", "5")
    val it = a.listIterator()
    it.next()
    assertFailsWith<NoSuchElementException> {
        while (true) {
            it.previous()
        }
    }
}

@Test fun runTest() {
    testIteratorNext()
    testIteratorPrevious()

    println("OK")
}