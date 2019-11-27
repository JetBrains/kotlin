// IGNORE_BACKEND_FIR: JVM_IR
var flag = true

fun exit(): Nothing = null!!

fun box(): String {
    val a: String
    if (flag) {
        a = "OK"
    }
    else {
        exit()
    }
    return a
}