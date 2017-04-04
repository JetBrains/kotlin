import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val data = listOf(null, "foo", null, "bar")
    val foo = data.filterNotNull()

    assertEquals(2, foo.size)
    assertEquals(listOf("foo", "bar"), foo)

    assertTrue {
        foo is List<String>
    }
}
