// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80588, KT-92047
// RENDER_DIAGNOSTICS_FULL_TEXT
// ^They don't support `OTHER_ERROR_WITH_REASON`.

sealed interface Key
class AlphaKey : Key
class BetaKey : Key

sealed interface Element<K : Key>
class Alpha : Element<AlphaKey>

class Container<T : Element<K>, K : Key>

typealias TA<A, B> = Container<A, B>

fun interface SamContainer<T : Element<K>, K : Key> {
    fun bar()
}

typealias SamTA<A, B> = SamContainer<A, B>

fun main() {
    Container<<!UPPER_BOUND_VIOLATED!>Alpha<!>, BetaKey>()
    Container<<!UPPER_BOUND_VIOLATED!>Alpha<!>, <!UPPER_BOUND_VIOLATED!>String<!>>()

    TA<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Alpha<!>, BetaKey>()
    TA<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Alpha<!>, <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>String<!>>()

    SamContainer<<!UPPER_BOUND_VIOLATED!>Alpha<!>, BetaKey> <!ARGUMENT_TYPE_MISMATCH!>{ }<!>
    SamContainer<<!UPPER_BOUND_VIOLATED!>Alpha<!>, <!UPPER_BOUND_VIOLATED!>String<!>> <!ARGUMENT_TYPE_MISMATCH!>{ }<!>
    SamTA<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Alpha<!>, BetaKey> { }
    SamTA<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Alpha<!>, <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>String<!>> { }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, sealed, typeConstraint,
typeParameter */
