// IGNORE_BACKEND_FIR: JVM_IR
fun exit(): Nothing = null!!

fun box(): String {
    val a: String
    try {
        a = "OK"
    }
    catch (e: Exception) {
        exit()
    }
    return a
}