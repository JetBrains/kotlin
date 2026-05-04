// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS +CompanionBlocksAndExtensions
//           (^ changes positioning of `WRONG_NUMBER_OF_TYPE_ARGUMENTS`)

class G<A>
typealias GtoG<B> = G<B>
typealias NGtoG = G<String>
companion fun G.foo() { }
companion val GtoG.bar: Unit get() { }

class NG
typealias GtoNG<C> = NG
companion fun NG.foo() { }
companion val GtoNG.bar: Unit get() { }

fun test() {
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>G<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GtoG<!>::bar
    NG::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GtoNG<!>::bar
    NGtoG::foo

    G<*>::bar
    GtoG<Any>::foo
    GtoNG<Nothing>::foo

    // wrong number
    NG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::foo
    NGtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::foo
    G<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::bar
    GtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::foo
    GtoNG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any, Nothing><!>::bar
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration, getter,
nullableType, propertyDeclaration, propertyWithExtensionReceiver, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
