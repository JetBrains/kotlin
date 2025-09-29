// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// ISSUE: KT-81251

private class EffectiveVisibility {
    val a: Any
        field: String = ""

    public inline fun foo() {
        a.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline, propertyDeclaration, stringLiteral */
