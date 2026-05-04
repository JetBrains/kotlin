// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS
//           (^ changes positioning of `WRONG_NUMBER_OF_TYPE_ARGUMENTS`)
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE: +ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

enum class NG { X }

typealias GtoNG<A> = NG

fun test() {
    GtoNG::values
    GtoNG::entries
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>GtoNG<*><!>::values
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>GtoNG<Any><!>::values

    // wrong number
    GtoNG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*, *><!>::values
    GtoNG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::entries
}

/* GENERATED_FIR_TAGS: callableReference, enumDeclaration, enumEntry, functionDeclaration, nullableType,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
