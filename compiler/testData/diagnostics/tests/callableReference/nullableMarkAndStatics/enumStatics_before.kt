// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -CompanionBlocksAndExtensions
// LANGUAGE: -ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

enum class C { X }

typealias TAtoC = C
typealias TAtoNC = C?

fun test() {
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>C<!>?::values
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>TAtoC<!>?::values
    TAtoNC::values
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>TAtoNC<!>?::values
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>C<!>?::entries
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>TAtoC<!>?::entries
    TAtoNC::entries
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>TAtoNC<!>?::entries
}

/* GENERATED_FIR_TAGS: callableReference, enumDeclaration, enumEntry, functionDeclaration, nullableType,
typeAliasDeclaration */
