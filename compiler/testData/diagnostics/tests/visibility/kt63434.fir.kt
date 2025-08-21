// RUN_PIPELINE_TILL: BACKEND
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
            if (<!USELESS_IS_CHECK!>b is Proto.Some<!>) return
        }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, ifExpression, isExpression,
localProperty, nestedClass, propertyDeclaration */
