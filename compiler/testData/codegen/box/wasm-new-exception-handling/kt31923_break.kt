// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved
// LANGUAGE: +ProperFinally
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