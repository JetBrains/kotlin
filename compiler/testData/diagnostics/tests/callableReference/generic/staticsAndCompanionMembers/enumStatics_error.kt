// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS
//           (^ changes positioning of `WRONG_NUMBER_OF_TYPE_ARGUMENTS`)
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE: +ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

enum class NG { X }

typealias GtoNG<A> = NG

fun test() {
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GtoNG<!>::values
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GtoNG<!>::entries
    GtoNG<*>::values
    GtoNG<Any>::values

    // wrong number
    GtoNG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*, *><!>::values
    GtoNG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::entries
}

/* GENERATED_FIR_TAGS: callableReference, enumDeclaration, enumEntry, functionDeclaration, nullableType,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
