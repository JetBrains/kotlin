// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT
// LANGUAGE: -ReportMissingUpperBoundsViolatedErrorOnAbbreviationAtSupertypes

interface I
open class TK<T : I, K : I>

typealias One<X> = TK<X, X>
typealias OneList<X> = List<TK<X, X>>
typealias Both<T, K> = TK<T, K>
typealias BothList<T, K> = List<TK<T, K>>

object O1 : One<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION, UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Any<!>>() // compiler error expected
object O2 : Both<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Any<!>, <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Any<!>>()

class A1<T : One<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION, UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Any<!>>>
class A2<T : One<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any, Any><!>>

interface IO1 : OneList<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>Any<!>> {}
interface IO2 : BothList<<!UPPER_BOUND_VIOLATED!>Any<!>, <!UPPER_BOUND_VIOLATED!>Any<!>> {}

fun foo1(x: One<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION, UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Any<!>>) {}
fun foo2(x: Both<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Any<!>, <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Any<!>>) {}

fun main() {
    One<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION, UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Any<!>>()
    Both<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Any<!>, <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Any<!>>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nullableType, objectDeclaration,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
