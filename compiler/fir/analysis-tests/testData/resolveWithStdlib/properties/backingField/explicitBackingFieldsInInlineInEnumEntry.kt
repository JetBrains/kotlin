// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -NOTHING_TO_INLINE, -OVERRIDE_BY_INLINE

enum class Some {
    A {
        override inline fun foo() {
            a.inc()
        }
        val a: Any field: Int = 1
    };
    open fun foo() {}
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration */
