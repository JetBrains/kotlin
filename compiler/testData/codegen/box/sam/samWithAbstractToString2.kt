fun interface SomeFun {
    override fun toString(): String
}

fun foo(o: Any) = o.toString()

fun box(): String {
    val o = SomeFun { "O" }
    val k = SomeFun { "K" }
    return o.toString() + foo(k)
}