// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE: -ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

open class A {
    inner class B
}

object C : A()
typealias D = C
typealias E<K> = C
typealias F = C?

class G {
   companion object : A()
}
typealias H = G?
typealias I<K> = G

fun test() {
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>C<!>?::B
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>D<!>?::B

    E::B
    E<*>::B
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>E<!>?::B
    E<*>?::<!UNSAFE_CALLABLE_REFERENCE!>B<!>

    F::B
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>F<!>?::B

    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>G<!>?::B
    H::B
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>H<!>?::B

    I::B
    I<*>::<!UNRESOLVED_REFERENCE!>B<!>
    I<*>?::<!UNRESOLVED_REFERENCE!>B<!>
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>I<!>?::B
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, functionDeclaration, inner, nullableType,
objectDeclaration, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
