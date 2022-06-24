// FIR_IDENTICAL
// SKIP_TXT

typealias Handler = suspend (String) -> Unit
suspend inline fun foo(handler: Handler) = Unit