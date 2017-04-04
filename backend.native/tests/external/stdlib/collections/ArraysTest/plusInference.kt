import kotlin.test.*

fun box() {
    val arrayOfArrays: Array<Array<out Any>> = arrayOf(arrayOf<Any>("s") as Array<out Any>)
    val elementArray = arrayOf<Any>("a") as Array<out Any>
    val arrayPlusElement: Array<Array<out Any>> = arrayOfArrays.plusElement(elementArray)
    assertEquals("a", arrayPlusElement[1][0])
    // ambiguity
    // val arrayPlusArray: Array<Array<out Any>> = arrayOfArrays + arrayOfArrays

    val arrayOfStringArrays = arrayOf(arrayOf("s"))
    val arrayPlusArray = arrayOfStringArrays + arrayOfStringArrays
    assertEquals("s", arrayPlusArray[1][0])
}
