annotation class Ann

data class Pair(val x: Int, val y: Int)

fun foo(): Int {
    @Ann val (a, b) = Pair(12, 34)
    @Err val (c, d) = Pair(56, 78)
    return a + b + c + d
}