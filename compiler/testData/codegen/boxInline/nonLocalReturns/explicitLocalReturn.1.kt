import test.*

fun test1(b: Boolean): String {
    val localResult = doCall @local {
        () : String ->
        if (b) {
            return@local "local"
        } else {
            return "nonLocal"
        }
    }

    return "localResult=" + localResult;
}

fun test2(nonLocal: String): String {
    val localResult = doCall {
        return nonLocal
    }
}

fun box(): String {
    val test1 = test1(true)
    if (test1 != "localResult=local") return "test1: ${test1}"

    val test2 = test1(false)
    if (test2 != "nonLocal") return "test2: ${test2}"

    return "OK"
}
