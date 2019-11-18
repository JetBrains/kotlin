// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

val log = StringBuilder()

fun logged(message: String, value: Int) =
    value.also { log.append(message) }

fun box(): String {
    var sum = 0
    for (i in (logged("start;", 4) downTo logged("end;", 1)).reversed().reversed()) {
        sum = sum * 10 + i
    }

    assertEquals(4321, sum)

    assertEquals("start;end;", log.toString())

    return "OK"
}