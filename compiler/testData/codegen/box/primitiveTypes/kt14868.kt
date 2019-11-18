// IGNORE_BACKEND_FIR: JVM_IR

fun box(): String {
    val x: Number = 75

    return "O" + x.toChar()
}