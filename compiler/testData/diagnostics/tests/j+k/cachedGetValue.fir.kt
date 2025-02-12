// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK

import java.util.concurrent.atomic.AtomicReference

class CacheOwner<T>(
    private val compute: () -> T,
) {
    private val cached = AtomicReference<CachedValue<T>?>(null)

    public fun getValue(): T = cached.updateAndGet { value ->
        when {
            value == null -> createNewCachedValue()
            value.isUpToDate() -> value
            else -> createNewCachedValue()
        }
    }!!.value

    private fun createNewCachedValue() = CachedValue(compute())
}

private class CachedValue<T>(val value: T) {
    fun isUpToDate() = true
}
