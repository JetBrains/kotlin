import kotlin.test.*

fun box() {
    assertEquals(true, "true".toBoolean())
    assertEquals(true, "True".toBoolean())
    assertEquals(false, "false".toBoolean())
    assertEquals(false, "not so true".toBoolean())
}
