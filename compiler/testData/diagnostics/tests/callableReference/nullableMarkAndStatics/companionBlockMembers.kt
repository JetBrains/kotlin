// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    companion {
        fun foo() { }
    }
}

typealias TAtoC = C
typealias TAtoNC = C?

fun test() {
    C?::foo
    TAtoC?::foo
    TAtoNC::foo
    TAtoNC?::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, nullableType, typeAliasDeclaration */
