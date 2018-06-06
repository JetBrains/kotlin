// IGNORE_BACKEND: JS
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=always-enable
// WITH_RUNTIME

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
    if (!checkTrue()) return "FAIL 0"
    if (!checkTrueWithMessage()) return "FAIL 1"
    try {
        checkFalse()
        return "FAIL 3"
    } catch (ignore: AssertionError) {
    }
    try {
        checkFalseWithMessage()
        return "FAIL 4"
    } catch (ignore: AssertionError) {
    }

    return "OK"
}