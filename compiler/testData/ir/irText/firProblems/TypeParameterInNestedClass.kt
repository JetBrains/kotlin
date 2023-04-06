// ISSUE: KT-58008

// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57429

object Retry {
    class Builder<B>(
        private val action: suspend () -> B,
    )

    fun <W> withExponentialBackoff(action: () -> W): Builder<W> {
        return Builder(action)
    }
}
