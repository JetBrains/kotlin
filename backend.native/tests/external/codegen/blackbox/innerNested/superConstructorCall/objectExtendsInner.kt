class A {
    open inner class Inner(val result: String)

    fun box(): String {
        val o = object : Inner("OK") {
            fun ok() = result
        }
        return o.ok()
    }
}

fun box() = A().box()
