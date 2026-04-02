// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT
// FIR_DUMP

abstract class InlineCompletionSessionManager {
    protected class Proto {
        class Some
    }
}

fun checkCannotAccess() {
    object : InlineCompletionSessionManager() {
        fun chch() {
            val b: Proto = Proto()
            if (<!IMPOSSIBLE_IS_CHECK_ERROR!>b is Proto.Some<!>) return
        }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, ifExpression, isExpression,
localProperty, nestedClass, propertyDeclaration */
