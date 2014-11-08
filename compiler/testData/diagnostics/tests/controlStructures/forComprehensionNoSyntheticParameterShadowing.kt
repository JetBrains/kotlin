data class Pair<out A, out B>(val first: A, val second: B)

fun Pair<Int, Int>.map(<!UNUSED_PARAMETER!>f<!>: (Pair<Int, Int>) -> Int): List<Int> = throw AssertionError("")

fun foo(): List<Int> = for ((i, j) in Pair(1, 2)) yield {
    val _p_ = i*j
    _p_ + 1
}