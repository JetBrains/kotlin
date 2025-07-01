// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78736
// DIAGNOSTICS: -NOTHING_TO_INLINE

inline fun foo() {
    object {
        inline fun inLocalObject() {}
    }
    <!NOT_YET_SUPPORTED_IN_INLINE!>fun<!> local() {}
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, inline, localFunction */
