interface UpdatableRendering<out T : UpdatableRendering<T>> {
    fun canUpdateFrom(another: @UnsafeVariance T): Boolean
}

internal fun Any.matchesRendering(other: Any): Boolean {
    return when {
        this::class != other::class -> false
        this !is UpdatableRendering<*> -> true
        else -> this.<!INAPPLICABLE_CANDIDATE!>canUpdateFrom<!>(other as UpdatableRendering<*>)
    }
}
