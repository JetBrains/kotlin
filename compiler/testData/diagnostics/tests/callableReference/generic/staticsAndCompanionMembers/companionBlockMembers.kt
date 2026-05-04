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
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>G<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GtoG<!>::foo
    NG::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GtoNG<!>::foo
    NGtoG::foo

    G<*>::foo
    GtoG<Any>::foo
    GtoNG<Nothing>::foo

    // wrong number
    NG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::foo
    NGtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::foo
    G<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::foo
    GtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::foo
    GtoNG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any, Nothing><!>::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, nullableType, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
