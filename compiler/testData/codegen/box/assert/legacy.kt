// TARGET_BACKEND: JVM
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=legacy
// WITH_RUNTIME
// FULL_JDK

import java.lang.reflect.Field
import java.lang.reflect.Modifier

fun setDesiredAssertionStatus(v: Boolean) {
    @Suppress("INVISIBLE_REFERENCE")
    val field = kotlin._Assertions.javaClass.getField("ENABLED")
    val modifiers = Field::class.java.getDeclaredField("modifiers");
    modifiers.isAccessible = true
    modifiers.setInt(field, field.modifiers and Modifier.FINAL.inv())
    field.set(null, v)
}

fun checkTrue(): Boolean {
    var hit = false
    val l = { hit = true; true }
    assert(l())
    return hit
}

fun checkTrueWithMessage(): Boolean {
    var hit = false
    val l = { hit = true; true }
    assert(l()) { "BOOYA!" }
    return hit
}

fun checkFalse(): Boolean {
    var hit = false
    val l = { hit = true; false }
    assert(l())
    return hit
}

fun checkFalseWithMessage(): Boolean {
    var hit = false
    val l = { hit = true; false }
    assert(l()) { "BOOYA!" }
    return hit
}

fun box(): String {
    setDesiredAssertionStatus(false)
    if (!checkTrue()) return "FAIL 0"
    setDesiredAssertionStatus(true)
    if (!checkTrue()) return "FAIL 1"

    setDesiredAssertionStatus(false)
    if (!checkTrueWithMessage()) return "FAIL 2"
    setDesiredAssertionStatus(true)
    if (!checkTrueWithMessage()) return "FAIL 3"

    setDesiredAssertionStatus(false)
    if (!checkFalse()) return "FAIL 4"
    setDesiredAssertionStatus(true)
    try {
        checkFalse()
        return "FAIL 5"
    } catch (ignore: AssertionError) {
    }

    setDesiredAssertionStatus(false)
    if (!checkFalseWithMessage()) return "FAIL 6"
    setDesiredAssertionStatus(true)
    try {
        checkFalseWithMessage()
        return "FAIL 7"
    } catch (ignore: AssertionError) {
    }

    return "OK"
}