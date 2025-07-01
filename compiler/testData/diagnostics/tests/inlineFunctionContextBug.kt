// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-78736
// DIAGNOSTICS: -NOTHING_TO_INLINE
// IGNORE_NON_REVERSED_RESOLVE
// IGNORE_REVERSED_RESOLVE
// IGNORE_PARTIAL_BODY_ANALYSIS
// ^They work correctly

inline fun foo() {
    object {
        inline fun inLocalObject() {}
    }
    <!NOT_YET_SUPPORTED_IN_INLINE!>fun<!> local() {}
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, inline, localFunction */
