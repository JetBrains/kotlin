// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test() {
    testInv()
    testIn()
    testOut()
    testStarProjection()
    testErrorType()
    testInProjection()
    testOutProjection()
    testDeeplyNested()
}

interface Bound1
interface Bound2
object First : Bound1, Bound2
object Second : Bound1, Bound2

class Inv<T>(val prop: T)
class Out<out O>(val prop: O)
class In<in I>(arg: I)
class BiParam<F, S>(first: F, second: S)

fun <S : Bound1> intersect(vararg elements: S): S = TODO()

fun makeStarProjection(): Inv<*> = TODO()
fun <I> makeInProjection(arg: I): Inv<in I> = TODO()
fun <O> makeOutProjection(arg: O): Inv<out O> = TODO()
fun testInv() = Inv(intersect(First, Second))
fun testOut() = Out(intersect(First, Second))
fun testIn() = In(intersect(First, Second))
fun testInProjection() = makeInProjection(intersect(First, Second))
fun testOutProjection() = makeOutProjection(intersect(First, Second))
fun testDeeplyNested() = Inv(Inv(Inv(intersect(First, Second))))

fun testStarProjection() = BiParam(
    intersect(First, Second),
    makeStarProjection()
)
fun testErrorType() = BiParam(
    intersect(First, Second),
    <!UNRESOLVED_REFERENCE!>unresolved<!>
)
