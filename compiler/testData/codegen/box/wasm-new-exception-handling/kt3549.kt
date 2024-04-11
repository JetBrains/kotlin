// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved
fun test1() : String {
    var s = "";
    try {
        try {
            s += "Try";
            throw Exception()
        } catch (x : Exception) {
            s += "Catch";
            throw x
        } finally {
            s += "Finally";
        }
    } catch (x : Exception) {
        return s
    }
}

fun test2() : String {
    var s = "";

    try {
        s += "Try";
        throw Exception()
    } catch (x : Exception) {
        s += "Catch";
    } finally {
        s += "Finally";
    }

    return s
}



fun box() : String {
    if (test1() != "TryCatchFinally") return "fail1: ${test1()}"

    if (test2() != "TryCatchFinally") return "fail2: ${test2()}"

    return "OK"
}
