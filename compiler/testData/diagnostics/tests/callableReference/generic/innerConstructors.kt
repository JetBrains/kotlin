// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS
//           (^ changes positioning of `WRONG_NUMBER_OF_TYPE_ARGUMENTS`)
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE: +ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

class G<A> {
    inner class Inner
}

typealias GtoG<B> = G<B>

class NG {
    inner class Inner
}

typealias GtoNG<C> = NG
typealias NGtoG = G<String>

fun test() {
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>G<!>::Inner
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GtoG<!>::Inner
    NG::Inner
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GtoNG<!>::Inner
    NGtoG::Inner

    G<*>::Inner
    GtoG<Any>::Inner
    GtoNG<Nothing>::Inner

    // wrong number
    NG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::Inner
    NGtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::Inner
    G<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::Inner
    GtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::Inner
    GtoNG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any, Nothing><!>::Inner
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, InnerClass, nullableType,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
