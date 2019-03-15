// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

val log = StringBuilder()

fun logged(message: String, value: Int) =
        value.also { log.append(message) }

fun box(): String {
    var s = 0
    for (i in (logged("start;", 2) downTo logged("end;", 1)).reversed()) {
        s += i
    }

    assertEquals(3, s)

    assertEquals("start;end;", log.toString())

    return "OK"
}