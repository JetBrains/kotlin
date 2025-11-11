// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80588
// RENDER_DIAGNOSTICS_FULL_TEXT
// IGNORE_NON_REVERSED_RESOLVE
// IGNORE_REVERSED_RESOLVE
// IGNORE_PARTIAL_BODY_ANALYSIS
// ^They don't support `OTHER_ERROR_WITH_REASON`.

sealed interface Key
class AlphaKey : Key
class BetaKey : Key

sealed interface Element<K : Key>
class Alpha : Element<AlphaKey>

class Container<T : Element<K>, K : Key>

typealias TA<A, B> = Container<A, B>

fun main() {
    Container<<!UPPER_BOUND_VIOLATED!>Alpha<!>, BetaKey>()
    Container<<!UPPER_BOUND_VIOLATED!>Alpha<!>, <!UPPER_BOUND_VIOLATED!>String<!>>()

    TA<<!UPPER_BOUND_VIOLATED!>Alpha<!>, BetaKey>()
    TA<<!UPPER_BOUND_VIOLATED!>Alpha<!>, <!UPPER_BOUND_VIOLATED!>String<!>>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, sealed, typeConstraint,
typeParameter */
