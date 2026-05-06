// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    fun test() {
        foo()
    }
}

companion fun C.foo() = ""

companion fun C.test() {
    foo()
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration, stringLiteral */
