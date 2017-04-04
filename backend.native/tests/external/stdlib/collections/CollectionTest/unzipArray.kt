import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val array = arrayOf(1 to 'a', 2 to 'b', 3 to 'c')
    val (ints, chars) = array.unzip()
    assertEquals(listOf(1, 2, 3), ints)
    assertEquals(listOf('a', 'b', 'c'), chars)
}
