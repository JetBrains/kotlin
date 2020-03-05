// IGNORE_BACKEND_FIR: JVM_IR
fun <T, V : T> f(x: V): V {
    fun g(y: V) = y
    return g(x)
}

fun box() = f<Any, String>("OK")
