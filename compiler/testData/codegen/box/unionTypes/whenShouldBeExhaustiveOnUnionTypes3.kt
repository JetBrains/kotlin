// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +UnionTypes

fun <T> select(a: T, b: T) : T = a

class A()

fun box(): String {
    val x = select("OK", 1L)
    val y = select(x, A())
    return when (x) {
        is CharSequence -> x.toString()
        is Number -> "Fail"
    }
}