object a
operator fun a.buildLiteral(body: B.() -> Unit): Int =
    B().apply(body).build()

class B {
    operator fun appendString(s: String) {}
    operator fun appendObject(x: Any) {}
    fun build() : Int = 0
}

fun test() {
    val arg = 1
    val result: Int = a"str $arg"
}