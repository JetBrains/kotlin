package lit

annotation class AnnInt(val a: IntArray)

@Suppress("UNSUPPORTED_FEATURE")
@AnnInt([1, 2])
fun foo() {}

@AnnInt(intArrayOf(1, 2))
fun bar() {}

fun main() {
    foo()
    bar()
}