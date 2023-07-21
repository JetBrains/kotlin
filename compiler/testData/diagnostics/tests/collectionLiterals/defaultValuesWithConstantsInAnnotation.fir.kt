const val ONE = 1

annotation class Foo(
        val a: IntArray = [ONE],
        val b: IntArray = [ONE, 2, 3]
)

val TWO = 2

fun getOne() = ONE
fun getTwo() = TWO

annotation class Bar(
        val a: IntArray = [TWO],
        val b: IntArray = [1, TWO],
        val c: IntArray = [getOne(), getTwo()]
)

annotation class Baz(
        val a: IntArray = [<!NULL_FOR_NONNULL_TYPE!>null<!>],
        val b: IntArray = [1, <!NULL_FOR_NONNULL_TYPE!>null<!>, 2],
        val c: IntArray = [<!NO_THIS!>this<!>]
)
