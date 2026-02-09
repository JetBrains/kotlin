// ISSUE: KT-83829
// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_PHASE_SUGGESTION

fun justCall(): () -> Unit = {}
<!NOTHING_TO_INLINE!>inline<!> fun inlineCall(): () -> Unit = {}

inline fun withJustCall(block: () -> Unit = justCall()) {
}

inline fun withInlineCall(block: () -> Unit = inlineCall()) {
}

inline fun noInlineAndCrossInline(
    noinline block1: () -> Unit = inlineCall(),
    noinline block2: () -> Unit = justCall(),
    crossinline block3: () -> Unit = inlineCall(),
    crossinline block4: () -> Unit = justCall(),
) {}

<!NOTHING_TO_INLINE!>inline<!> fun inNonFunctional(
    block1: Any = inlineCall(),
    block2: Any = justCall(),
) {}

/* GENERATED_FIR_TAGS: crossinline, functionDeclaration, functionalType, inline, lambdaLiteral, noinline */
