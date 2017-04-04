import kotlin.test.*

fun box() {
    val arr1 = intArrayOf(1, 2, 3, 4, 5)
    val iter1 = arr1.asIterable()
    assertEquals(arr1.toList(), iter1.toList())
    arr1[0] = 0
    assertEquals(arr1.toList(), iter1.toList())

    val arr2 = arrayOf("one", "two", "three")
    val iter2 = arr2.asIterable()
    assertEquals(arr2.toList(), iter2.toList())
    arr2[0] = ""
    assertEquals(arr2.toList(), iter2.toList())

    val arr3 = IntArray(0)
    val iter3 = arr3.asIterable()
    assertEquals(iter3.toList(), emptyList<Int>())

    val arr4 = Array(0, { "$it" })
    val iter4 = arr4.asIterable()
    assertEquals(iter4.toList(), emptyList<String>())
}
