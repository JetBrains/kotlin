object id

operator fun id.buildLiteral(body: LiteralBuilder.() -> Unit): Int {
    return LiteralBuilder().apply(body).build()
}

class LiteralBuilder {
    operator fun appendString(s: String) { }
    operator fun appendObject(obj: Any) { }
    fun build(): Int = 0
}

fun test() {
    val a = 1
    val b = 2
    val x: Int = id"str $a ${b}"
}