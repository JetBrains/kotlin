// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE: -ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

class C {
    companion object {
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

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, functionDeclaration, nullableType,
objectDeclaration, typeAliasDeclaration */
