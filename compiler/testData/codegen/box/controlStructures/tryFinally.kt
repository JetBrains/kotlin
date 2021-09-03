var log = ""

fun foo() {
    try {
        log += "1"
        mightThrow()
        log += "2"
    } finally {
        log += "3"
        "FINALLY"
    }

    val t = try {
        log += "4"
        mightThrow2()
        log += "5"
    } finally {
        log += "6"
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

fun test() {
    log += "a"
    foo()
    throw2 = true
    log += " b"
    foo()
    // Never gets here.
    throw1 = true
    log += " c"
    foo()
}


fun box(): String {
    try {
        test()
    } catch (e: Exception) {}
    if (log != "a123456 b12346")
        return "Failed: $log"
    return "OK"
}
