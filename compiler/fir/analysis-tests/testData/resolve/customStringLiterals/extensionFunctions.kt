object a
class B
operator fun a.buildLiteral(body: B.() -> Unit) {}
operator fun B.appendString(s: String) {}
operator fun B.appendObject(x: Any) {}

fun test() {
    val arg = 1
    a"str $arg"
}