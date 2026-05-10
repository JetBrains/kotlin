// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    companion {
        fun foo() { }
    }
}

typealias TAtoC = C
typealias TAtoNC = C?

fun test() {
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>C<!>?::foo
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>TAtoC<!>?::foo
    TAtoNC::foo
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_ERROR!>TAtoNC<!>?::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, nullableType, typeAliasDeclaration */
