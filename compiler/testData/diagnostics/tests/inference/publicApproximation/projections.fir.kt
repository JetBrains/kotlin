// DIAGNOSTICS: -UNUSED_PARAMETER

interface Bound1
interface Bound2
object First : Bound1, Bound2
object Second : Bound1, Bound2

class Inv<T>

fun <O: Bound1> makeOut(vararg args: O): Inv<out O> = TODO()
fun <I: Bound1> makeIn(vararg args: I): Inv<in I> = TODO()

fun testOut() = makeOut(First, Second)
fun testIn() = makeIn(First, Second)

fun test() {
    testOut()
    testIn()
}
