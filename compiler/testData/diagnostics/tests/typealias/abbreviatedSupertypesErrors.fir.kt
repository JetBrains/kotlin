// SKIP_TXT
// !LANGUAGE: +ReportMissingUpperBoundsViolatedErrorOnAbbreviationAtSupertypes

interface I
open class TK<T : I, K : I>

typealias One<X> = TK<X, X>
typealias OneList<X> = List<TK<X, X>>
typealias Both<T, K> = TK<T, K>
typealias BothList<T, K> = List<TK<T, K>>

object O1 : <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>One<Any><!>() // compiler error expected
object O2 : <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Both<Any, Any><!>()

class A1<T : <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>One<Any><!>>
class A2<T : <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>One<Any, Any><!>>

interface IO1 : OneList<Any> {}
interface IO2 : BothList<Any, Any> {}

fun foo1(x: <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>One<Any><!>) {}
fun foo2(x: <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Both<Any, Any><!>) {}

fun main() {
    <!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>One<Any>()<!>
    <!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>Both<Any, Any>()<!>
}
