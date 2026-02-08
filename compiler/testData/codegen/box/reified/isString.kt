package foo
import kotlin.test.*

inline fun <reified T> isInstance(x: Any?): Boolean =
    x is T

// CHECK_NOT_CALLED: isInstance

fun box(): String {
    assertEquals(true, isInstance<String>(""))
    assertEquals(true, isInstance<String>("a"))

    assertEquals(true, isInstance<String?>(""), "isInstance<String?>(\"\")")
    assertEquals(true, isInstance<String?>(null), "isInstance<String?>(null)")
    assertEquals(false, isInstance<String?>(10), "isInstance<String?>(10)")

    return "OK"
}