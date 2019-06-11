// !LANGUAGE: +ProperFinally
var result = ""

fun test() {
    try {
        try {
            try {
                result += "try"
                return
            } catch (fail: Throwable) {
                result += " catch"
            }
        } catch (e: Throwable) {
            result += " finally_catch"
        } finally {
            result += " finally 1"
            throw RuntimeException("Fail 1")
        }
    } finally {
        result += " finally 2"
        throw RuntimeException("Fail 2")
    }
}

fun box(): String {
    try {
        test()
        return "fail: expected exception"
    } catch (e: RuntimeException) {
        if (e.message != "Fail 2") return "wrong exception: ${e.message}"
    }

    return if (result == "try finally 1 finally 2") "OK" else "fail: $result"
}