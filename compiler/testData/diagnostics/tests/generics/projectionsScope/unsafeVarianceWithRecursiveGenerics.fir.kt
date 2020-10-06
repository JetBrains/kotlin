interface UpdatableRendering<out T : UpdatableRendering<T>> {
    fun canUpdateFrom(another: @UnsafeVariance T): Boolean
}

internal fun Any.matchesRendering(other: Any): Boolean {
    return when {
        this::class != other::class -> false
        this !is UpdatableRendering<*> -> true
        else -> this.canUpdateFrom(other as UpdatableRendering<*>)
    }
}
