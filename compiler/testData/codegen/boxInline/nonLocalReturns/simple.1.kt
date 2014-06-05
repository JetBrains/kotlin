import test.*

fun test1(local: Int, nonLocal: String, doNonLocal: Boolean): String {

    val localResult = doCall {
        if (doNonLocal) {
            return nonLocal
        }
        local
    }

    if (localResult == 11) {
        return "OK_LOCAL"
    }
    else {
        return "LOCAL_FAILED"
    }
}

fun test2(local: Int, nonLocal: String, doNonLocal: Boolean): String {

    val localResult = doCall {
        if (doNonLocal) {
            return@test2 nonLocal
        }
        local
    }

    if (localResult == 11) {
        return "OK_LOCAL"
    }
    else {
        return "LOCAL_FAILED"
    }
}

fun box(): String {
    var test1 = test1(11, "fail", false)
    if (test1 != "OK_LOCAL") return "test1: ${test1}"

    test1 = test1(-1, "OK_NONLOCAL", true)
    if (test1 != "OK_NONLOCAL") return "test2: ${test1}"

    var test2 = test2(11, "fail", false)
    if (test2 != "OK_LOCAL") return "test1: ${test2}"

    test2 = test2(-1, "OK_NONLOCAL", true)
    if (test2 != "OK_NONLOCAL") return "test2: ${test2}"

    return "OK"
}
