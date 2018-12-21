// IGNORE_BACKEND: JVM_IR
fun box(): String {
    val z = 1;
    return "O" + "K".toString() + z
}

// 1 LDC "OK"
// 1 LDC