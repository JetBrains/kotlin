// WITH_RUNTIME
import kotlin.test.*

val log = StringBuilder()

fun logged(message: String, value: Int) =
        value.also { log.append(message) }

fun box(): String {
    var s = 0
    for (i in (logged("start;", 1) until logged("end;", 3)).reversed()) {
        s += i
    }

    assertEquals(3, s)

    assertEquals("start;end;", log.toString())

    return "OK"
}