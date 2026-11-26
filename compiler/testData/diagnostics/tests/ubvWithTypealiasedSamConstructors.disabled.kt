// RUN_PIPELINE_TILL: CODEGEN
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
    SamTA<Alpha, BetaKey> { } // no diagnostic
    SamTA<Alpha, String> { } // no diagnostic
}

/* GENERATED_FIR_TAGS: classDeclaration, funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral,
nullableType, sealed, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
