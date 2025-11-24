// DONT_TARGET_EXACT_BACKEND: JVM_IR
// REASON: KT-78644
// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

// FILE: a.kt
inline fun inlineFun1(p: () -> Unit) {
    p()
    <!INLINE_CALL_CYCLE!>inlineFun2(p)<!>
}

// FILE: b.kt
inline fun inlineFun2(p: () -> Unit) {
    p()
    <!INLINE_CALL_CYCLE!>inlineFun1(p)<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline */
