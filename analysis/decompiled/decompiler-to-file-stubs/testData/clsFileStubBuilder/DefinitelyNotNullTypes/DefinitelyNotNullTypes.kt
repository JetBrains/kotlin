class DefinitelyNotNullTypes<T>(private val x: T & Any) {
    fun foo(xs: List<T & Any>): T & Any {
        return if (xs.isNotEmpty()) xs[0] else x
    }

    fun <R> bar(x: R & Any, action: (R & Any) -> R & Any): R & Any {
        return action(x)
    }
}
