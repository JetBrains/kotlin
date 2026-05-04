// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE: +ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

class C { companion object }
typealias TAtoC = C
typealias TAtoNC = C?
fun C.Companion.foo() { }
fun C.Companion?.bar() { }

fun test() {
    C?::foo
    TAtoC?::foo
    TAtoNC::foo
    TAtoNC?::foo

    C?::bar
    TAtoC?::bar
    TAtoNC::bar
    TAtoNC?::bar
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, funWithExtensionReceiver,
functionDeclaration, nullableType, objectDeclaration, typeAliasDeclaration */
