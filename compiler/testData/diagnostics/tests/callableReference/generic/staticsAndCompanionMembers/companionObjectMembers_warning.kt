// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS
//           (^ changes positioning of `WRONG_NUMBER_OF_TYPE_ARGUMENTS`)
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE: -ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

class G<A> {
    companion object {
        fun foo() { }
    }
}

typealias GtoG<B> = G<B>

class NG {
    companion object {
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

    G<*>::<!UNRESOLVED_REFERENCE!>foo<!>
    GtoG<Any>::<!UNRESOLVED_REFERENCE!>foo<!>
    GtoNG<Nothing>::<!UNRESOLVED_REFERENCE!>foo<!>

    // wrong number
    NG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::<!UNRESOLVED_REFERENCE!>foo<!>
    NGtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::<!UNRESOLVED_REFERENCE!>foo<!>
    G<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::<!UNRESOLVED_REFERENCE!>foo<!>
    GtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::<!UNRESOLVED_REFERENCE!>foo<!>
    GtoNG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any, Nothing><!>::<!UNRESOLVED_REFERENCE!>foo<!>
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, functionDeclaration, nullableType,
objectDeclaration, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
