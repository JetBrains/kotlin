// WITH_RUNTIME
import kotlin.test.*

fun intRange() = 4 .. 1
fun longRange() = 4L .. 1L
fun charRange() = '4' .. '1'

fun box(): String {
    for (i in intRange().reversed()) {
        throw AssertionError("Loop should not be executed")
    }

    for (i in longRange().reversed()) {
        throw AssertionError("Loop should not be executed")
    }

    for (i in charRange().reversed()) {
        throw AssertionError("Loop should not be executed")
    }

    return "OK"
}