// !RENDER_DIAGNOSTICS_FULL_TEXT
fun <T> flowOf(value: T): Flow<T> = TODO()

interface FlowCollector<in T> {}

interface Flow<out T>

fun <T, R> Flow<T>.transform(transform: FlowCollector<R>.(T) -> Unit): Flow<R> = TODO()

fun f() {
    fun <T> doEmit(collector: FlowCollector<T>) {}
    flowOf(1).<!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>transform<!> { doEmit(this) }
}
