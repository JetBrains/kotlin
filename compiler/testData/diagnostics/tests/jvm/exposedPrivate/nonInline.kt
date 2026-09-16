// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -NOTHING_TO_INLINE

private class C

private inline fun privateFun() { C() }

internal fun test() {
    privateFun()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline */
