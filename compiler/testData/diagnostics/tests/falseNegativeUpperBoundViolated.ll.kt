// LL_FIR_DIVERGENCE
// LL runners don't support `OTHER_ERROR_WITH_REASON`.
// LL_FIR_DIVERGENCE
// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80588
// RENDER_DIAGNOSTICS_FULL_TEXT

sealed interface Key
class AlphaKey : Key
class BetaKey : Key

sealed interface Element<K : Key>
class Alpha : Element<AlphaKey>

class Container<T : Element<K>, K : Key>

typealias TA<A, B> = Container<A, B>

fun main() {
    Container<Alpha, <!UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>BetaKey<!>>()
    Container<Alpha, <!UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>String<!>>()

    TA<Alpha, <!UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>BetaKey<!>>()
    TA<Alpha, <!UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>String<!>>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, sealed, typeConstraint,
typeParameter */
