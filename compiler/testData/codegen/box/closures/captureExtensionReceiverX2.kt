// IGNORE_BACKEND_FIR: JVM_IR
fun String.f(x: String): String {
    fun String.g() = { this@f + this@g }()
    return x.g()
}

fun box() = "O".f("K")
