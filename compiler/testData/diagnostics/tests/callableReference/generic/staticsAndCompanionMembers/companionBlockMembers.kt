// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS +CompanionBlocksAndExtensions
//           (^ changes positioning of `WRONG_NUMBER_OF_TYPE_ARGUMENTS`)

class G<A> {
    companion {
        fun foo() { }
    }
}

typealias GtoG<B> = G<B>

class NG {
    companion {
        fun foo() { }
    }
}

typealias GtoNG<C> = NG
typealias NGtoG = G<String>

fun test() {
    G::foo
    GtoG::foo
    NG::foo
    GtoNG::foo
    NGtoG::foo

    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>G<*><!>::foo
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>GtoG<Any><!>::foo
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>GtoNG<Nothing><!>::foo

    // wrong number
    NG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::foo
    NGtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::foo
    G<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::foo
    GtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::foo
    GtoNG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any, Nothing><!>::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, nullableType, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
