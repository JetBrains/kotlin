// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS
//           (^ changes positioning of `WRONG_NUMBER_OF_TYPE_ARGUMENTS`)
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE: -ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

class G<A> {
    class Other
    typealias Nested = Other
}

typealias GtoG<B> = G<B>

class TopLevel

class NG {
    typealias Nested = TopLevel
}

typealias GtoNG<C> = NG
typealias NGtoG = G<String>

fun test() {
    G::Nested
    GtoG::Nested
    NG::Nested
    GtoNG::Nested
    NGtoG::Nested

    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>G<*><!>::Nested
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>GtoG<Any><!>::Nested
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>GtoNG<Nothing><!>::Nested

    // wrong number
    NG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::Nested
    NGtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::Nested
    G<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::Nested
    GtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::Nested
    GtoNG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any, Nothing><!>::Nested
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, nestedClass, nullableType,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
