// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -NOTHING_TO_INLINE

private class C {
    fun f(): C? = null
    internal inline fun test() { f() }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline, nullableType */
