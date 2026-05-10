// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE: +ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

enum class C { X }

typealias TAtoC = C
typealias TAtoNC = C?

fun test() {
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>C<!>?::values
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>TAtoC<!>?::values
    TAtoNC::values
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>TAtoNC<!>?::values
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>C<!>?::entries
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>TAtoC<!>?::entries
    TAtoNC::entries
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>TAtoNC<!>?::entries
}

/* GENERATED_FIR_TAGS: callableReference, enumDeclaration, enumEntry, functionDeclaration, nullableType,
typeAliasDeclaration */
