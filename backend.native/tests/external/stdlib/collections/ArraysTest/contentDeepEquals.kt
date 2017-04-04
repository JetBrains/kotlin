import kotlin.test.*

fun box() {
    val arr1 = arrayOf("a", 1, intArrayOf(2))
    val arr2 = arrayOf("a", 1, intArrayOf(2))
    assertFalse(arr1 contentEquals arr2)
    assertTrue(arr1 contentDeepEquals arr2)
    arr2[2] = arr1
    assertFalse(arr1 contentDeepEquals arr2)
}
