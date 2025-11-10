// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80588

sealed interface Key
class AlphaKey : Key
class BetaKey : Key

sealed interface Element<K : Key>
class Alpha : Element<AlphaKey>

class Container<T : Element<K>, K : Key, U: T>
typealias TA<A, B, C> = Container<A, B, C>

fun someFunc(it: Any?) {}

fun main() {
    Container<<!UPPER_BOUND_VIOLATED!>Alpha<!>, BetaKey, _>()
    Container<<!UPPER_BOUND_VIOLATED!>Alpha<!>, <!UPPER_BOUND_VIOLATED!>String<!>, _>()

    TA<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Alpha<!>, BetaKey, _>()
    TA<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Alpha<!>, <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>String<!>, _>()

    <!INAPPLICABLE_CANDIDATE!>someFunc<!>(TA<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Alpha<!>, BetaKey, _>())
    <!INAPPLICABLE_CANDIDATE!>someFunc<!>(TA<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Alpha<!>, <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>String<!>, _>())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, sealed, typeConstraint,
typeParameter */
