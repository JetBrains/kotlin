// WITH_RUNTIME

data class A(val a: Int, val b: Int)

class B(val a: Int, val b: Int)

fun box(): String {
    val a = 10
    val x = Any();
    val b = when (x) {
        is A(val a, val b: Int) -> a
        is Pair(val c, val d) -> a + a
        is A(val a) -> a
        is Pair(val a: Int, val b: Int) -> a + b
        else -> return "OK"
    }
    return "FAIL"
}