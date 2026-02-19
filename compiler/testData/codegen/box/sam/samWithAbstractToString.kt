fun interface SomeFun {
    override fun toString(): String
}

fun box(): String {
    val ok = SomeFun { "OK" }
    return ok.toString()
}