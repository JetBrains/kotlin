// IGNORE_BACKEND_FIR: JVM_IR
fun test(str: String): String {
    var s = ""
    for (i in 1..3) {
        s += if (i<2) str else break
    }
    return s
}

fun box(): String = test("OK")