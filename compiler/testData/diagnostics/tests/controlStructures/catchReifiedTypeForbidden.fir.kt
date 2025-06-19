// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -AllowReifiedTypeInCatchClause
// KT-54363

inline fun <reified E : Exception, R> tryCatch(lazy: () -> R, failure: (E) -> R): R =
    try {
        lazy()
    } catch (<!UNSUPPORTED_FEATURE!>e: E<!>) {
        failure(e)
    }

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, localProperty, nullableType, propertyDeclaration,
reified, tryExpression, typeConstraint, typeParameter */

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, localProperty, nullableType, propertyDeclaration,
reified, tryExpression, typeConstraint, typeParameter */
