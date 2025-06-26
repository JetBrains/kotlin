// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL

inline fun foo(x: Int = <!INLINE_CALL_CYCLE!>bar()<!>): Int = x

inline fun bar(x: Int = <!INLINE_CALL_CYCLE!>foo()<!>): Int = x

inline fun qux(x: Int = quz(42)): Int = x

inline fun quz(x: Int = qux(42)): Int = x

/* GENERATED_FIR_TAGS: functionDeclaration, inline, integerLiteral */
