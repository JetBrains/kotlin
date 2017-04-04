import kotlin.test.*

fun box() {
    val arr1 = intArrayOf(0, 1, 2, 3, 4)
    assertEquals(4, arr1.lastIndex)
    assertEquals(4, arr1[arr1.lastIndex])

    val arr2 = Array<String>(5, { "$it" })
    assertEquals(4, arr2.lastIndex)
    assertEquals("4", arr2[arr2.lastIndex])
}
