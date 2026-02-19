// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// SKIP_TXT

typealias Handler = suspend (String) -> Unit
suspend inline fun foo(handler: Handler) = Unit

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, suspend, typeAliasDeclaration */
