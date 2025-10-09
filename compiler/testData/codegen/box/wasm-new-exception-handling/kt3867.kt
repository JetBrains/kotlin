// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved
fun fail() = if (true) throw RuntimeException() else 1

fun test1(): String {
    var r = ""
    try {
        try {
            r += "Try"
            return r
        } catch (e: RuntimeException) {
            r += "Catch"
            return r
        }
        finally {
            r += "Finally"
            fail()
        }
    } catch (e: RuntimeException) {
        return r
    }
}

fun test2(): String {
    var r = ""
    try {
        try {
            r += "Try"
        } catch (e: RuntimeException) {
            r += "Catch"
        }
        finally {
            r += "Finally"
            fail()
        }
    } catch (e: RuntimeException) {
        return r
    }
    return r + "Fail"
}

fun box(): String {
   if (test1() != "TryFinally") return "fail1: ${test1()}"

   if (test2() != "TryFinally") return "fail2: ${test2()}"

   return "OK"
}