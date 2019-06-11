// IGNORE_BACKEND_FIR: JVM_IR
var result = ""

fun test() {
    try {
        for (z in 1..2) {

            try {
                result += "try "
                continue
            } catch (fail: Throwable) {
                result += " catch"
            }
        }
        result += "after loop"
    } finally {
        result += " finally"
        throw RuntimeException()
    }
}

fun box(): String {
    try {
        test()
        return "fail: expected exception"
    } catch (e: RuntimeException) {

    }

    return if (result == "try try after loop finally") "OK" else "fail: $result"
}