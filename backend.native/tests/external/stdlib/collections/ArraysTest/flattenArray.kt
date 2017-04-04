import kotlin.test.*

fun box() {
    val arr1: Array<Array<Int>> = arrayOf(arrayOf(1, 2, 3), arrayOf(4, 5, 6))
    val arr2: Array<out Array<Int>> = arr1
    val arr3: Array<out Array<out Int>> = arr1
    val arr4: Array<Array<out Int>> = arr1 as Array<Array<out Int>>

    val expected = listOf(1, 2, 3, 4, 5, 6)
    assertEquals(expected, arr1.flatten())
    assertEquals(expected, arr2.flatten())
    assertEquals(expected, arr3.flatten())
    assertEquals(expected, arr4.flatten())
}
