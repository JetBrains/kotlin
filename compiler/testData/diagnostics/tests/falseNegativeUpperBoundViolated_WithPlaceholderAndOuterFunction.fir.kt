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
    Container<Alpha, BetaKey, _>()
    Container<Alpha, String, _>()

    TA<Alpha, BetaKey, _>()
    TA<Alpha, String, _>()

    <!INAPPLICABLE_CANDIDATE!>someFunc<!>(TA<Alpha, BetaKey, _>())
    <!INAPPLICABLE_CANDIDATE!>someFunc<!>(TA<Alpha, String, _>())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, sealed, typeConstraint,
typeParameter */
