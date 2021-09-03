var log = ""

fun foo() {
    try {
        log += "1"
        mightThrow()
        log += "2"
    } catch (e: Throwable) {
        log += "3"
        return
    }

    val t = try {
        log += "4"
        mightThrow2()
        log += "5"
    } catch (e: Throwable) {
        log += "6"
        return
    }

    val x = try {
        log += "7"
        mightThrow3()
        log += "8"
    } catch (e: Throwable) {
        log += "9"
        return
    }
}

var throw1 = false
var throw2 = false
var throw3 = false

fun mightThrow() {
    if (throw1) throw Exception()
}

fun mightThrow2() {
    if (throw2) throw Exception()
}

fun mightThrow3(): Int {
    if (throw3) throw Exception()
    return 42
}

fun box(): String {
    log += "a"
    foo()
    throw3 = true
    log += " b"
    foo()
    throw2 = true
    log += " c"
    foo()
    throw1 = true
    log += " d"
    foo()

    if (log != "a124578 b124579 c1246 d13")
        return "Failed: $log"

    return "OK"
}
