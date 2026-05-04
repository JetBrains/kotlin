// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -CompanionBlocksAndExtensions
// LANGUAGE: -ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

enum class C { X }

typealias TAtoC = C
typealias TAtoNC = C?

fun test() {
    C?::values
    TAtoC?::values
    TAtoNC::values
    TAtoNC?::values
    C?::entries
    TAtoC?::entries
    TAtoNC::entries
    TAtoNC?::entries
}

/* GENERATED_FIR_TAGS: callableReference, enumDeclaration, enumEntry, functionDeclaration, nullableType,
typeAliasDeclaration */
