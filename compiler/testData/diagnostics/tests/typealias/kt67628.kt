// FIR_IDENTICAL
// DIAGNOSTICS: -UNREACHABLE_CODE, -USELESS_ELVIS

typealias Void = Nothing?

interface IteratorResult<TReturn>

class IteratorYieldResult : IteratorResult<Void>

suspend fun hasNext(): Boolean {
    val firstResult: IteratorResult<*> = TODO()
    val lastResult: IteratorResult<*> = TODO()

    val result = lastResult ?: firstResult

    return result is IteratorYieldResult
}
