// IGNORE_BACKEND_FIR: JVM_IR
class A {
    val result: String
    init {
        val flag = "OK"
        fun getFlag(): String = flag
        result = { getFlag() } ()
    }
}
fun box(): String = A().result
