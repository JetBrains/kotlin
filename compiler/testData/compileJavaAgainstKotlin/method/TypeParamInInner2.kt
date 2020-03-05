package test

fun <V> f(x: V): Int {
    fun g(y: V) = 2
    return g(x)
}
