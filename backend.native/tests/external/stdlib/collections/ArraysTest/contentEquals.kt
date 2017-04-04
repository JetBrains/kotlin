import kotlin.test.*

fun box() {
    val arr1 = arrayOf("a", 1, null)
    val arr2 = arrayOf(*arr1)
    assertTrue(arr1 contentEquals arr2)
    val arr3 = arr2.reversedArray()
    assertFalse(arr1 contentEquals arr3)
}
