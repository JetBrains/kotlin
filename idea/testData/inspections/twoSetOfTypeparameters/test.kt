// KT-12623 ISE: Type parameter ... not found for public fun ...

fun <T> boo<X>() {}

fun <T, C: MutableCollection<T>> Collection<partitionTo(first: C, second: C, f:(T) -> Boolean): Pair<C, C> = TODO()