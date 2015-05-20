import test.*

fun test1(nonLocal: String): String {
    val localResult = doCall<String> {
        return nonLocal
    }

    return "NON_LOCAL_FAILED"
}


fun box(): String {
    return test1("OK")
}