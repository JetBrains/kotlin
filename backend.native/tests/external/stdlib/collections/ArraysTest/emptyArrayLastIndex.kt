import kotlin.test.*

fun box() {
    val arr1 = IntArray(0)
    assertEquals(-1, arr1.lastIndex)

    val arr2 = emptyArray<String>()
    assertEquals(-1, arr2.lastIndex)
}
