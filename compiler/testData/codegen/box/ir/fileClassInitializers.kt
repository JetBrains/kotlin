// IGNORE_BACKEND_FIR: JVM_IR
//WITH_RUNTIME
import kotlin.test.assertEquals

val x = 1
val y = x + 1

fun box(): String {
    assertEquals(x, 1)
    assertEquals(y, 2)

    return "OK"
}