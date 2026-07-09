// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// ISSUE: KT-87372

open class C {
    companion {
        protected fun foo() {}
        protected val bar = ""
    }
}

class D : C() {
    fun test() {
        C.foo()
        C.bar
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, propertyDeclaration, stringLiteral */
