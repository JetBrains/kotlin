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

fun main() {
    if (!checkTrue()) error("FAIL 1")
    if (!checkTrueWithMessage()) error("FAIL 3")

    try {
        checkFalse()
        error("FAIL 5")
    } catch (ignore: AssertionError) {
    }

    try {
        checkFalseWithMessage()
        error("FAIL 7")
    } catch (ignore: AssertionError) {
    }
}
