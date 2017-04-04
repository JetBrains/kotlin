import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    assertTrue(arrayListOf(1) is RandomAccess, "ArrayList is RandomAccess")
    assertTrue(listOf(1, 2) is RandomAccess, "Default read-only list implementation is RandomAccess")
    assertTrue(listOf(1) is RandomAccess, "Default singleton list is RandomAccess")
    assertTrue(emptyList<Int>() is RandomAccess, "Empty list is RandomAccess")
}
