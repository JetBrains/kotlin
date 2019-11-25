// IGNORE_BACKEND_FIR: JVM_IR
var flag = true

object Test {
    val magic: Nothing get() = null!!
}

fun box(): String {
    val a: String
    if (flag) {
        a = "OK"
    }
    else {
        Test.magic
    }
    return a
}