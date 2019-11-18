// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
fun failAtRuntime(numberArg: Number = 0.0): Number {
    return numberArg
}

fun box(): String {
    failAtRuntime()
    return "OK"
}