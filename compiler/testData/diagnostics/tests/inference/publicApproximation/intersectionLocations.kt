// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test() {
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Bound1>")!>testInv()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("In<Bound1>")!>testIn()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<Bound1>")!>testOut()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("BiParam<Bound1, Inv<*>>")!>testStarProjection()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("[Error type: Return type for function cannot be resolved]")!>testErrorType()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in Bound1>")!>testInProjection()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Bound1>")!>testOutProjection()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Inv<Inv<Bound1>>>")!>testDeeplyNested()<!>
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
