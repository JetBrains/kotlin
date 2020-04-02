// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

val log = StringBuilder()

fun logged(message: String, value: Int) =
    value.also { log.append(message) }

fun box(): String {
    var sum = 0
    for (i in (logged("start;", 8) downTo logged("end;", 1)).reversed() step logged("step;", 2)) {
        sum = sum * 10 + i
    }

    assertEquals(1357, sum)

    assertEquals("start;end;step;", log.toString())

    return "OK"
}