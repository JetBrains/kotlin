// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE

private class C

private inline fun privateFun() = C()

internal open class A {
    public inline fun test() {
        privateFun()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline */
