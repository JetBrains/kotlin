// IGNORE_BACKEND_FIR: JVM_IR
inline fun <T> tryOrElse(f1: () -> T, f2: () -> T): T {
    try {
        return f1()
    }
    catch (e: Exception) {
        return f2()
    }
}

fun testIt() = "abc" + tryOrElse({ "def" }, { "oops" }) + "ghi"

fun box(): String {
    val test = testIt()
    if (test != "abcdefghi") return "Failed, test==$test"

    return "OK"
}
