// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -CompanionBlocksAndExtensions
// LANGUAGE: -ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

class C {
    companion object {
        fun foo() { }
    }
}

typealias TAtoC = C
typealias TAtoNC = C?

fun test() {
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>C<!>?::foo
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>TAtoC<!>?::foo
    TAtoNC::foo
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>TAtoNC<!>?::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, functionDeclaration, nullableType,
objectDeclaration, typeAliasDeclaration */
