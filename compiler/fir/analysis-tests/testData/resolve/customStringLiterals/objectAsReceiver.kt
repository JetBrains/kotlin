object a {
    operator fun buildLiteral(body: B.() -> Unit) {}
}

class B {
    operator fun appendString(s: String) {}
    operator fun appendObject(x: Any) {}
}

fun test() {
    val arg = 1
    a"str $arg"
}