import kotlin.test.assertEquals

fun box(): String {
    assertEquals("deprecated", deprecated::class.simpleName)

    return "OK"
}