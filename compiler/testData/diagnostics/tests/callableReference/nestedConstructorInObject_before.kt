// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS
// LANGUAGE: -CompanionBlocksAndExtensions
// LANGUAGE: -ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

object Obj {
    class Nested
}

typealias Regular = Obj
typealias Generic<K> = Obj
typealias Nullable = Obj?
typealias GenericNullable<K> = Obj?

fun test() {
    Obj::Nested
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>Obj<!>?::Nested
    Regular::Nested
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>Regular<!>?::Nested
    Generic::Nested
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Generic<!>?::Nested
    Generic<Any>::Nested
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>Generic<Any><!>?::Nested
    Nullable::Nested
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>Nullable<!>?::Nested
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GenericNullable<!>::Nested
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GenericNullable<!>?::Nested
    GenericNullable<Any>::Nested
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>GenericNullable<Any><!>?::Nested
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, nestedClass, nullableType,
objectDeclaration, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
