// IGNORE_BACKEND_FIR: JVM_IR
data class Z(val p: String, val k: String)


fun create(p: Boolean): Z? {
    return if (p) {
        Z("O", "K")
    }
    else {
        null;
    }
}

fun test(p: Boolean): String {
    val (a, b) = create(p) ?: return "null"
    return a + b
}

fun box(): String {
    if (test(false) != "null") return "fail 1: ${test(false)}"

    return test(true)
}