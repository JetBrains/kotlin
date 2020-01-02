// !LANGUAGE: -ProperFinally
// TARGET_BACKEND: JVM
var result = ""

fun test() {
    try {
        try {
            result += "try"
            return
        } catch (fail: Throwable) {
            result += " catch"
        }
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

    return if (result == "try finally catch finally") "OK" else "fail: $result"
}