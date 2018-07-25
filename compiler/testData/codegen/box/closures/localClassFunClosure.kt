// IGNORE_BACKEND: JVM_IR
fun box(): String {
    val o = "O"
    fun ok() = o + "K"
    class OK {
        val ok = ok()
    }
    return OK().ok
}