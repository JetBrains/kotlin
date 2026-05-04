// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C
typealias TAtoC = C
typealias TAtoNC = C?
companion fun C.foo() { }

fun test() {
    C?::foo
    TAtoC?::foo
    TAtoNC::foo
    TAtoNC?::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration, nullableType,
typeAliasDeclaration */
