// FIR_IDENTICAL
// ISSUE: KT-57889

class SafeResult<S>

inline fun <T> checkNotEdt(body: (SafeResult<T>) -> Nothing) {}

private fun <V> getNonEdt(): SafeResult<V> {
    checkNotEdt { return it }
    return SafeResult()
}
