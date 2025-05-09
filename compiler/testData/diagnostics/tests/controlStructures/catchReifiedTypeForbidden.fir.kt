// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -AllowReifiedTypeInCatchClause
// KT-54363

inline fun <reified E : Exception, R> tryCatch(lazy: () -> R, failure: (E) -> R): R =
    try {
        lazy()
    } catch (<!UNSUPPORTED_FEATURE!>e: E<!>) {
        failure(e)
    }
