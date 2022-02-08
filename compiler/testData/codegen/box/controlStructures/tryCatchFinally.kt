var log = ""

fun foo() {
    try {
        log += "1"
        mightThrow()
        log += "2"
    } catch (e: Throwable) {
        log += "3"
        "OK"
    } finally {
        log += "4"
        "FINALLY"
    }

    val t = try {
        log += "5"
        mightThrow2()
        log += "6"
    } catch (e: Throwable) {
        log += "7"
        "OK2"
    } finally {
        log += "8"
        "FINALLY2"
    }
}

var throw1 = false
var throw2 = false

fun mightThrow() {
    if (throw1) throw Exception()
}

fun mightThrow2() {
    if (throw2) throw Exception()
}

fun box(): String {
    log += "a"
    foo()
    throw2 = true
    log += " b"
    foo()
    throw1 = true
    log += " c"
    foo()
    if (log != "a124568 b124578 c134578")
        return "Failed: $log"
    return "OK"
}