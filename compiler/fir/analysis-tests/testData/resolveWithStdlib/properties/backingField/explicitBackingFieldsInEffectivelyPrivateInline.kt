// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// ISSUE: KT-81251

private class EffectiveVisibility {
    val a: Any
        field: String = ""

    public inline fun foo() {
        a.length
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline, propertyDeclaration, stringLiteral */
