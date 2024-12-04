// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

fun unsupportedEx() {
    if (true) throw UnsupportedOperationException()
}

fun runtimeEx() {
    if (true) throw RuntimeException()
}

fun test1WithFinally() : String {
    var s = "";
    try {
        try {
            s += "Try";
            unsupportedEx()
        } finally {
            s += "Finally"
        }
    } catch (x : RuntimeException) {
        return s
    }
    return s + "Failed"
}


fun test2WithFinally() : String {
    var s = "";
    try {
        try {
            s += "Try";
            unsupportedEx()
            return s
        } finally {
            s += "Finally"
        }
    } catch (x : RuntimeException) {
        return s
    }
}

fun box() : String {
    if (test1WithFinally() != "TryFinally") return "fail2: ${test1WithFinally()}"

    if (test2WithFinally() != "TryFinally") return "fail4: ${test2WithFinally()}"
    return "OK"
}