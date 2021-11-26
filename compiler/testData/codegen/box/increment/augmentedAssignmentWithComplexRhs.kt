// WITH_STDLIB
import kotlin.test.*

var log = ""
var result = 20
var doubleResult = 40.0

fun <T> id(value: T) = value

fun box(): String {
    result += if (id("true") == "true") {
        result += 10
        log += "true chosen;"
        3
    }
    else {
        4
    }

    assertEquals(23, result)

    doubleResult += if (id("true") == "true") {
        doubleResult += 100
        log += "true chosen;"
        2
    }
    else {
        5
    }

    assertEquals(42, (doubleResult + 0.1).toInt())
    assertEquals("true chosen;true chosen;", log)

    return "OK"
}