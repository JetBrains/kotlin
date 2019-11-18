// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

@JvmName("Fail")
fun OK() {}

fun box() = ::OK.name
