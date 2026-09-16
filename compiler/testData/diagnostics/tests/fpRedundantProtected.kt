// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-66161

private open class A {
    protected fun test() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
