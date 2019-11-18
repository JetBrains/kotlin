// IGNORE_BACKEND_FIR: JVM_IR
class A<in I>(init_o: I, private val init_k: I) {
    private val o: I = init_o
    private fun k(): I = init_k

    fun getOk() = o.toString() + k().toString()
}

fun box(): String {
    val a = A("O", "K")
    return a.getOk()
}
