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
    C?::B
    D?::B

    E::B
    E<*>::B
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>E<!>?::B
    E<*>?::<!UNSAFE_CALLABLE_REFERENCE!>B<!>

    F::B
    F?::B

    G?::B
    H::B
    H?::B

    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>I<!>::B
    I<*>::<!UNRESOLVED_REFERENCE!>B<!>
    I<*>?::<!UNRESOLVED_REFERENCE!>B<!>
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>I<!>?::B
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, functionDeclaration, inner, nullableType,
objectDeclaration, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
