// SKIP_TXT
// LANGUAGE: -ReportMissingUpperBoundsViolatedErrorOnAbbreviationAtSupertypes

interface I
open class TK<T : I, K : I>

typealias One<X> = TK<X, X>
typealias OneList<X> = List<TK<X, X>>
typealias Both<T, K> = TK<T, K>
typealias BothList<T, K> = List<TK<T, K>>

object O1 : <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_WARNING!>One<Any><!>() // compiler error expected
object O2 : <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Both<Any, Any><!>()

class A1<T : <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_WARNING!>One<Any><!>>
class A2<T : One<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any, Any><!>>

interface IO1 : <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>OneList<Any><!> {}
interface IO2 : <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_WARNING!>BothList<Any, Any><!> {}

fun foo1(x: One<<!UPPER_BOUND_VIOLATED!>Any<!>>) {}
fun foo2(x: Both<<!UPPER_BOUND_VIOLATED!>Any<!>, <!UPPER_BOUND_VIOLATED!>Any<!>>) {}

fun main() {
    One<<!UPPER_BOUND_VIOLATED!>Any<!>>()
    Both<<!UPPER_BOUND_VIOLATED!>Any<!>, <!UPPER_BOUND_VIOLATED!>Any<!>>()
}
