fun unsupportedEx() {
    if (true) throw UnsupportedOperationException()
}

fun runtimeEx() {
    if (true) throw RuntimeException()
}

fun test1() : String {
    var s = "";
    try {
        try {
            s += "Try";
            unsupportedEx()
        } catch (x : UnsupportedOperationException) {
            s += "Catch";
            runtimeEx()
        } catch (e: RuntimeException) {
            s += "WrongCatch"
        }
    } catch (x : RuntimeException) {
        return s
    }
    return s + "Failed"
}

fun test1WithFinally() : String {
    var s = "";
    try {
        try {
            s += "Try";
            unsupportedEx()
        } catch (x : UnsupportedOperationException) {
            s += "Catch";
            runtimeEx()
        } catch (e: RuntimeException) {
            s += "WrongCatch"
        } finally {
            s += "Finally"
        }
    } catch (x : RuntimeException) {
        return s
    }
    return s + "Failed"
}

fun test2() : String {
    var s = "";
    try {
        try {
            s += "Try";
            unsupportedEx()
            return s
        } catch (x : UnsupportedOperationException) {
            s += "Catch";
            runtimeEx()
            return s
        } catch (e: RuntimeException) {
            s += "WrongCatch"
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
        } catch (x : UnsupportedOperationException) {
            s += "Catch";
            runtimeEx()
            return s
        } catch (e: RuntimeException) {
            s += "WrongCatch"
        } finally {
            s += "Finally"
        }
    } catch (x : RuntimeException) {
        return s
    }
    return s + "Failed"
}



fun box() : String {
    if (test1() != "TryCatch") return "fail1: ${test1()}"
    if (test1WithFinally() != "TryCatchFinally") return "fail2: ${test1WithFinally()}"

    if (test2() != "TryCatch") return "fail3: ${test2()}"
    if (test2WithFinally() != "TryCatchFinally") return "fail4: ${test2WithFinally()}"
    return "OK"
}
