// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-92047
// LANGUAGE_FEATURE_TOGGLED: ReportUpperBoundViolatedWithSamTypealiasedConstructors

sealed interface Key
class AlphaKey : Key
class BetaKey : Key

sealed interface Element<K : Key>
class Alpha : Element<AlphaKey>

fun interface SamContainer<T : Element<K>, K : Key> {
    fun bar()
}

typealias SamTA<A, B> = SamContainer<A, B>

fun main() {
    SamTA<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Alpha<!>, BetaKey> { } // no diagnostic
    SamTA<<!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>Alpha<!>, <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>String<!>> { } // no diagnostic
}

/* GENERATED_FIR_TAGS: classDeclaration, funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral,
nullableType, sealed, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
