// FIR_IDENTICAL
// ISSUE: KT-83829
// RUN_PIPELINE_TILL: FRONTEND

fun justCall(): () -> Unit = {}
<!NOTHING_TO_INLINE!>inline<!> fun inlineCall(): () -> Unit = {}

inline fun withJustCall(block: () -> Unit = <!INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE!>justCall()<!>) {
}

inline fun withInlineCall(block: () -> Unit = <!INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE!>inlineCall()<!>) {
}

inline fun noInlineAndCrossInline(
    noinline block1: () -> Unit = inlineCall(),
    noinline block2: () -> Unit = justCall(),
    crossinline block3: () -> Unit = <!INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE!>inlineCall()<!>,
    crossinline block4: () -> Unit = <!INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE!>justCall()<!>,
) {}

<!NOTHING_TO_INLINE!>inline<!> fun inNonFunctional(
    block1: Any = inlineCall(),
    block2: Any = justCall(),
) {}

/* GENERATED_FIR_TAGS: crossinline, functionDeclaration, functionalType, inline, lambdaLiteral, noinline */
