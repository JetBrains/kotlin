// WITH_RUNTIME
import kotlin.test.*

var log = ""
var result = 20

fun <T> id(value: T) = value

fun box(): String {
    result += if (id("true") == "true") {
        result += 10
        log += "true chosen"
        3
    }
    else {
        4
    }

    assertEquals(23, result)
    assertEquals("true chosen", log)

    return "OK"
}