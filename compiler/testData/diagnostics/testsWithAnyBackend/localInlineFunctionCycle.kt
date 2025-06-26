// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL

inline fun test() {
    object {
        inline fun localInline() = <!INLINE_CALL_CYCLE!>test()<!>
        fun localNotInline() = <!INLINE_CALL_CYCLE!>test()<!>

        inline fun localInline2() = "OK"
        fun localNotInline2() = localInline2()

        inline fun test2() {
            object {
                inline fun localInline3() = <!INLINE_CALL_CYCLE!>test()<!>
                inline fun localInline4() = <!INLINE_CALL_CYCLE!>test2()<!>

                fun localNotInline3() = <!INLINE_CALL_CYCLE!>test()<!>
                fun localNotInline4() = <!INLINE_CALL_CYCLE!>test2()<!>
            }
        }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, inline, stringLiteral */
