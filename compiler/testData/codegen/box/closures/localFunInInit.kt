class A {
    val result: String
    init {
        val flag = "OK"
        fun getFlag(): String = flag
        result = { getFlag() }.let { it() }
    }
}
fun box(): String = A().result
