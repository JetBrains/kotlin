// IGNORE_BACKEND_K2: ANY
// ^KT-67283
fun interface SomeFun {
    override fun toString(): String
}

fun box(): String {
    val ok = SomeFun { "OK" }
    return ok.toString()
}