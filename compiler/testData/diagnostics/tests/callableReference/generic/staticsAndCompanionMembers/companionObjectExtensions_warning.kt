// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS
//           (^ changes positioning of `WRONG_NUMBER_OF_TYPE_ARGUMENTS`)
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE: -ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

class G<A> { companion object }
typealias GtoG<B> = G<B>
typealias NGtoG = G<String>
fun G.Companion.foo() { }

class NG { companion object }
typealias GtoNG<C> = NG
fun NG.Companion.foo() { }

fun test() {
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>G<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GtoG<!>::foo
    NG::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GtoNG<!>::foo
    NGtoG::foo

    G<*>::<!NONE_APPLICABLE!>foo<!>
    GtoG<Any>::<!NONE_APPLICABLE!>foo<!>
    GtoNG<Nothing>::<!NONE_APPLICABLE!>foo<!>

    // wrong number
    NG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::<!NONE_APPLICABLE!>foo<!>
    NGtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::<!NONE_APPLICABLE!>foo<!>
    G<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::<!NONE_APPLICABLE!>foo<!>
    GtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::<!NONE_APPLICABLE!>foo<!>
    GtoNG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any, Nothing><!>::<!NONE_APPLICABLE!>foo<!>
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, funWithExtensionReceiver,
functionDeclaration, getter, nullableType, objectDeclaration, propertyDeclaration, propertyWithExtensionReceiver,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
