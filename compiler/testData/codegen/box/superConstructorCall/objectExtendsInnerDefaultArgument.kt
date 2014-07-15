class A {
    open inner class Inner(val result: String = "OK", val int: Int)

    fun box(): String {
        val o = object : Inner(int = 0) {
            fun ok() = result
        }
        return o.ok()
    }
}

fun box() = A().box()
