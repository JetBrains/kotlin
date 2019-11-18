// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val f = "KOTLIN"::get
    return "${f(1)}${f(0)}"
}
