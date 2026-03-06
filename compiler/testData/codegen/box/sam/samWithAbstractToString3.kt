fun interface SomeFun {
    override fun toString(): String
}

fun foo(o: Any) = o.toString()

fun box(): String {
    val oLambda: () -> String = { "O" }
    val kLambda: () -> String = { "K" }
    val o = SomeFun(oLambda)
    val k = SomeFun(kLambda)
    return o.toString() + foo(k)
}