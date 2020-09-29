// IGNORE_BACKEND_FIR: JVM_IR
fun f(s: String) = "$s"

fun g(s: String?) = "$s"

// 2 valueOf
// 0 NEW java/lang/StringBuilder