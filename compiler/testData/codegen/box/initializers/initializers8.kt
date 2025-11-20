// WITH_STDLIB
import kotlin.test.*

var globalString = "OK"

fun box(): String {
    return globalString
}
