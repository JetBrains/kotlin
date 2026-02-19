// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true
// KT-66084
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB
import kotlin.test.*

fun box(): String {
    try {
        val x = cast<String>(Any())
        return "FAIL: ${x.length}"
    } catch (e: ClassCastException) {
        return "OK"
    }
}

fun <T> cast(x: Any?) = x as T
