// SKIP_TXT
// !LANGUAGE: -ReportMissingUpperBoundsViolatedErrorOnAbbreviationAtSupertypes

interface I
open class TK<T : I, K : I>

typealias One<X> = TK<X, X>
typealias OneList<X> = List<TK<X, X>>
typealias Both<T, K> = TK<T, K>
typealias BothList<T, K> = List<TK<T, K>>

object O1 : One<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>Any<!>>() // compiler error expected
object O2 : Both<<!UPPER_BOUND_VIOLATED!>Any<!>, <!UPPER_BOUND_VIOLATED!>Any<!>>()

class A1<T : One<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>Any<!>>>
class A2<T : One<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>Any<!>, Any>>

interface IO1 : OneList<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>Any<!>> {}
interface IO2 : BothList<<!UPPER_BOUND_VIOLATED!>Any<!>, <!UPPER_BOUND_VIOLATED!>Any<!>> {}

fun foo1(x: One<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>Any<!>>) {}
fun foo2(x: Both<<!UPPER_BOUND_VIOLATED!>Any<!>, <!UPPER_BOUND_VIOLATED!>Any<!>>) {}

fun main() {
    One<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>Any<!>>()
    Both<<!UPPER_BOUND_VIOLATED!>Any<!>, <!UPPER_BOUND_VIOLATED!>Any<!>>()
}
