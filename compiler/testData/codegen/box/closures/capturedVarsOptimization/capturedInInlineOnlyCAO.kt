// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

fun box(): String {
    var x = ""
    run {
        x += "O"
        x += "K"
    }
    return x
}