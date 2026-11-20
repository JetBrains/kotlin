// LL_FIR_DIVERGENCE
// FIR checkers don't raise backend's INLINE_CALL_CYCLE diagnostic.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -NOTHING_TO_INLINE
// DONT_WARN_ON_ERROR_SUPPRESSION

inline fun test() {
    object {
        inline fun localInline() = test()
        @Suppress("RECURSION_IN_INLINE")
        fun localNotInline() = test()

        inline fun localInline2() = "OK"
        fun localNotInline2() = localInline2()

        inline fun test2() {
            object {
                inline fun localInline3() = test()
                inline fun localInline4() = test2()

                fun localNotInline3() = test()
                @Suppress("RECURSION_IN_INLINE")
                fun localNotInline4() = test2()
            }
        }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, inline, stringLiteral */
