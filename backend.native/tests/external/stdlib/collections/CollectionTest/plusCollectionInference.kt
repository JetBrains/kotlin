import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val listOfLists = listOf(listOf("s"))
    val elementList = listOf("a")
    val result: List<List<String>> = listOfLists.plusElement(elementList)
    assertEquals(listOf(listOf("s"), listOf("a")), result, "should be list + element")

    val listOfAny = listOf<Any>("a") + listOf<Any>("b")
    assertEquals(listOf("a", "b"), listOfAny, "should be list + list")

    val listOfAnyAndList = listOf<Any>("a") + listOf<Any>("b") as Any
    assertEquals(listOf("a", listOf("b")), listOfAnyAndList, "should be list + Any")
}
