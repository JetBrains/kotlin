// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-60604

private const val MESSAGE = "This is deprecated"

@Deprecated(MESSAGE)
inline fun hello(f: () -> Int): Int = f()

/* GENERATED_FIR_TAGS: const, functionDeclaration, functionalType, inline, propertyDeclaration, stringLiteral */
