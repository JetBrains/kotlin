// !LANGUAGE: +ProperFinally
var result = ""

fun test() {
    for (z in 1..2) {
        try {
            try {
                result += "try"
                continue
            } catch (fail: Throwable) {
                result += "catch"
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