import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val list = arrayListOf("foo", "bar")
    assertEquals("foo", list[0])
    assertEquals("bar", list[1])

    // lists throw an exception if out of range
    assertFails {
        val outOfBounds = list[2]
    }

    // lets try update the list
    list[0] = "new"
    list[1] = "thing"

    // lists don't allow you to set past the end of the list
    assertFails {
        list[2] = "works"
    }

    list.add("works")
    assertEquals(listOf("new", "thing", "works"), list)
}
