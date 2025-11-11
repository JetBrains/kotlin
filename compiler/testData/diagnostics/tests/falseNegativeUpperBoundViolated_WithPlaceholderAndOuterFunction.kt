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
    Container<Alpha, <!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>BetaKey<!>, _>()
    Container<Alpha, <!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>String<!>, _>()

    TA<Alpha, <!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>BetaKey<!>, _>()
    TA<Alpha, <!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>String<!>, _>()

    someFunc(TA<Alpha, <!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>BetaKey<!>, _>())
    someFunc(TA<Alpha, <!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>String<!>, _>())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, sealed, typeConstraint,
typeParameter */
