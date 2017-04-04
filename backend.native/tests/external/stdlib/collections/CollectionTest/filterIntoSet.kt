import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = listOf("foo", "bar")
    val foo = data.filterTo(hashSetOf<String>()) { it.startsWith("f") }

    assertTrue {
        foo.all { it.startsWith("f") }
    }
    assertEquals(1, foo.size)
    assertEquals(hashSetOf("foo"), foo)

    assertTrue {
        foo is HashSet<String>
    }
}
