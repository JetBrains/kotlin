// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

val log = StringBuilder()

fun logged(message: String, value: Int) =
    value.also { log.append(message) }

fun box(): String {
    var sum = 0
    for (i in ((logged("start;", 1) until logged("end;", 9)).reversed() step logged("step;", 2)).reversed()) {
        sum = sum * 10 + i
    }

    assertEquals(2468, sum)

    assertEquals("start;end;step;", log.toString())

    return "OK"
}