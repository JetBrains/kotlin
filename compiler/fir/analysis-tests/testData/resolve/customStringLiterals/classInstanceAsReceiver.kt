class A {
    operator fun buildLiteral(body: B.() -> Unit) {}
}

class B {
    operator fun appendString(s: String) {}
    operator fun appendObject(x: Any) {}
}

fun test(a: A) {
    val arg = 1
    a"str $arg"
}