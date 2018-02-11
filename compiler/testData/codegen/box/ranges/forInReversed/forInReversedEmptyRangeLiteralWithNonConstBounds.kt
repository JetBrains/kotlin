// WITH_RUNTIME
import kotlin.test.*

fun intLow() = 4
fun intHigh() = 1
fun longLow() = 4L
fun longHigh() = 1L
fun charLow() = '4'
fun charHigh() = '1'

fun box(): String {
    for (i in (intLow() .. intHigh()).reversed()) {
        throw AssertionError("Loop should not be executed")
    }

    for (i in (longLow() .. longHigh()).reversed()) {
        throw AssertionError("Loop should not be executed")
    }

    for (i in (charLow() .. charHigh()).reversed()) {
        throw AssertionError("Loop should not be executed")
    }

    return "OK"
}