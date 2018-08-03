//WITH_RUNTIME
//IGNORE_BACKEND: JS_IR
import kotlin.test.assertEquals

val x = 1
val y = x + 1

fun box(): String {
    assertEquals(x, 1)
    assertEquals(y, 2)

    return "OK"
}