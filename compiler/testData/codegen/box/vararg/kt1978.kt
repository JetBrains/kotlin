// IGNORE_BACKEND_FIR: JVM_IR
fun aa(vararg a : String): String = a[0]

fun box(): String {
    var result: String = ""
    var i = 1
    while (3 > i++) {
        result = aa(if (true) "OK" else "fail")
    }
    return result
}
