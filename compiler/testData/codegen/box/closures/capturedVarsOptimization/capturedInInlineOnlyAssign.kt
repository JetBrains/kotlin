// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

fun box(): String {
    var x = ""
    run { x = "OK" }
    return x
}