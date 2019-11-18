// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

val log = StringBuilder()

fun logged(message: String, value: Int) =
    value.also { log.append(message) }

fun box(): String {
    var sum = 0
    for (i in ((logged("start;", 1)..logged("end;", 10)).reversed() step logged("step2;", 2)).reversed() step logged("step3;", 3)) {
        sum = sum * 10 + i
    }

    assertEquals(258, sum)

    assertEquals("start;end;step2;step3;", log.toString())

    return "OK"
}