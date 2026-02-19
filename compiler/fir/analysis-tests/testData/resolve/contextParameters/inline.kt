// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

context(<!CONTEXT_PARAMETER_MUST_BE_NOINLINE!>ctx: () -> Unit<!>) inline fun foo1() {
    val ctxSink = ctx
}

context(noinline ctx: () -> Unit) <!NOTHING_TO_INLINE!>inline<!> fun foo2() {
    val ctxSink = ctx
}

context(<!CONTEXT_PARAMETER_MUST_BE_NOINLINE!>crossinline ctx: () -> Unit<!>) inline fun foo3() {
    val ctxSink = ctx
}

context(ctx: String) <!NOTHING_TO_INLINE!>inline<!> fun bar1() {
    val ctxSink = ctx
}

context(<!ILLEGAL_INLINE_PARAMETER_MODIFIER!>noinline<!> ctx: String) <!NOTHING_TO_INLINE!>inline<!> fun bar2() {
    val ctxSink = ctx
}

context(<!ILLEGAL_INLINE_PARAMETER_MODIFIER!>noinline<!> ctx: String) <!NOTHING_TO_INLINE!>inline<!> fun bar3() {
    val ctxSink = ctx
}

/* GENERATED_FIR_TAGS: crossinline, functionDeclaration, functionDeclarationWithContext, functionalType, inline,
localProperty, noinline, propertyDeclaration */
