import kotlin.test.*

fun box() {
    val genericArray: Array<Int> = arrayOf(1, 2, 3)
    val primitiveArray: IntArray = genericArray.toIntArray()
    expect(3) { primitiveArray.size }
    assertEquals(genericArray.asList(), primitiveArray.asList())


    val charList = listOf('a', 'b')
    val charArray: CharArray = charList.toCharArray()
    assertEquals(charList, charArray.asList())
}
