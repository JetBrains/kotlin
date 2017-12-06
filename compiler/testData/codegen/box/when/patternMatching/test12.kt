// WITH_RUNTIME

data class A(val a: Int, val b: Int)

class B(val a: Int, val b: Int)

fun box(): String {
    val a = 10
    val x = Any();
    val b = when (x) {
        match A(a, b: Int) -> a
        match Pair<*, *>(c, d) -> a + a
        match A(a) -> a
        match Pair<*, *>(a: Int, b: Int) -> a + b
        else -> return "OK"
    }
    return "FAIL"
}