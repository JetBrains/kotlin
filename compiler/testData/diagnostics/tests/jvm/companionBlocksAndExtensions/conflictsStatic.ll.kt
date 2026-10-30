// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `CONFLICTING_JVM_DECLARATIONS`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB

class A {

    companion object {
        @JvmStatic
        val a: String = "companionObjectVal"
        @JvmStatic
        fun foo(s: String) = "companionObjectFun: $s"
    }
    companion {
        val a: String = "companionBlockVal"
        fun foo(s: String) = "companionBlockFun: $s"
    }

}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, propertyDeclaration,
stringLiteral */
