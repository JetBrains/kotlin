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
    Container<<!UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>Alpha<!>, BetaKey, _>()
    Container<<!UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>Alpha<!>, <!UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>String<!>, _>()

    TA<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_DEPRECATION_WARNING!>Alpha<!>, BetaKey, _>()
    TA<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_DEPRECATION_WARNING!>Alpha<!>, <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_DEPRECATION_WARNING!>String<!>, _>()

    <!INAPPLICABLE_CANDIDATE!>someFunc<!>(TA<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_DEPRECATION_WARNING!>Alpha<!>, BetaKey, _>())
    <!INAPPLICABLE_CANDIDATE!>someFunc<!>(TA<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_DEPRECATION_WARNING!>Alpha<!>, <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_DEPRECATION_WARNING!>String<!>, _>())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, sealed, typeConstraint,
typeParameter */
