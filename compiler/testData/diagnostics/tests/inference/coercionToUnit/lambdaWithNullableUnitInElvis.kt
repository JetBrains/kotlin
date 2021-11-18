// FIR_IDENTICAL
// SKIP_TXT

fun nullableF(): (() -> Unit)?= null

fun String.unit() {}

fun foo(x: String?): () -> Unit = nullableF() ?: { x?.unit() }
