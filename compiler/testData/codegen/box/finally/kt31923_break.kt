// !LANGUAGE: +ProperFinally
// IGNORE_BACKEND_FIR: JVM_IR
var result = ""

fun test() {
    for (z in 1..2) {
        try {
            try {
                result += "try"
                break
            } catch (fail: Throwable) {
                result += " catch"
            }
        } finally {
            result += " finally"
            throw RuntimeException()
        }
    }
}

fun box(): String {
    try {
        test()
        return "fail: expected exception"
    } catch (e: RuntimeException) {

    }

    return if (result == "try finally") "OK" else "fail: $result"
}