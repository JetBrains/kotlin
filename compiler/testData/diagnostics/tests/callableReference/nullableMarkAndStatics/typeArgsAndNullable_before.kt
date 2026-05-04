// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS
// LANGUAGE: -CompanionBlocksAndExtensions
// LANGUAGE: -ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

class C<A> {
    companion object {
        fun cmpFoo() { }
    }
    class Nested
}

typealias TAtoC<B> = C<B>
typealias TAtoNC<B> = C<B>?

fun C.Companion.extFoo() { }

enum class CE { X }
typealias TAtoСE = CE
typealias TAtoNCE = CE?

fun test() {
    C<*>?::<!UNRESOLVED_REFERENCE!>cmpFoo<!>
    TAtoC<Any>?::<!UNRESOLVED_REFERENCE!>cmpFoo<!>
    TAtoNC<Any>::<!UNRESOLVED_REFERENCE!>cmpFoo<!>
    TAtoNC<Any>?::<!UNRESOLVED_REFERENCE!>cmpFoo<!>

    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>C<!>?::cmpFoo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TAtoC<!>?::cmpFoo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TAtoNC<!>::cmpFoo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TAtoNC<!>?::cmpFoo

    C<*>?::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>extFoo<!>
    TAtoC<Any>?::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>extFoo<!>
    TAtoNC<Any>::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>extFoo<!>
    TAtoNC<Any>?::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>extFoo<!>

    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>C<!>?::extFoo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TAtoC<!>?::extFoo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TAtoNC<!>::extFoo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TAtoNC<!>?::extFoo

    C<*>?::Nested
    TAtoC<Any>?::Nested
    TAtoNC<Any>::Nested
    TAtoNC<Any>?::Nested

    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>C<!>?::Nested
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TAtoC<!>?::Nested
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TAtoNC<!>::Nested
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TAtoNC<!>?::Nested

    CE?::values
    TAtoСE?::values
    TAtoNCE::values
    TAtoNCE?::values
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, enumDeclaration, enumEntry,
funWithExtensionReceiver, functionDeclaration, nestedClass, nullableType, objectDeclaration, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
