import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    val original = arrayListOf(1, 2, 3, 4)
    val reversedSubList = original.asReversed().subList(1, 3)

    assertEquals(listOf(3, 2), reversedSubList)
    reversedSubList.clear()
    assertEquals(emptyList<Int>(), reversedSubList)
    assertEquals(listOf(1, 4), original)

    reversedSubList.add(100)
    assertEquals(listOf(100), reversedSubList)
    assertEquals(listOf(1, 100, 4), original)
}
