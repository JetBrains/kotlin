// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    fun String.f() = this
    val vf: String.() -> String = { this }

    val localExt = "O".f() + "K"?.f()
    if (localExt != "OK") return "localExt $localExt"

    return "O".vf() + "K"?.vf()
}