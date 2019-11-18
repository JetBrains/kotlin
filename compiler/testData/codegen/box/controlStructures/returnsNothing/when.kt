// IGNORE_BACKEND_FIR: JVM_IR
fun exit(): Nothing = null!!

var x = 0

fun box(): String {
    val a: String
    when (x) {
        0 -> a = "OK"
        1 -> a = "???"
        2 -> exit()
        else -> exit()
    }
    return a
}