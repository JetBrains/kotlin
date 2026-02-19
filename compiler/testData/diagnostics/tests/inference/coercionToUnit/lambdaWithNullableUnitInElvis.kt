// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// SKIP_TXT

fun nullableF(): (() -> Unit)?= null

fun String.unit() {}

fun foo(x: String?): () -> Unit = nullableF() ?: { x?.unit() }

/* GENERATED_FIR_TAGS: elvisExpression, funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral,
nullableType, safeCall */
