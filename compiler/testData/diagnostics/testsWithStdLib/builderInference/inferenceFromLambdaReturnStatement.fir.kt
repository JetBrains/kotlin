// ISSUE: KT-57889

class SafeResult<S>

inline fun <T> checkNotEdt(body: (SafeResult<T>) -> Nothing) {}

private fun <V> getNonEdt(): SafeResult<V> {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>checkNotEdt<!> { return it }
    return SafeResult()
}
