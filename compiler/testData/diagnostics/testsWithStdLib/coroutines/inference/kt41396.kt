// FIR_IDENTICAL
// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE_ERROR -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS

fun <T, R> Flow<T>.transformLatest(@BuilderInference transform: suspend FlowCollector<R>.(value: T) -> Unit) = null as Flow<R>

interface Flow<out T> {
    suspend fun collect(collector: FlowCollector<T>)
}

interface FlowCollector<in T> {
    suspend fun emit(value: T)
}

fun <T> flow(@BuilderInference block: suspend FlowCollector<T>.() -> Unit) = null as Flow<T>
fun <T> flowOf(value: T) = null as Flow<T>

fun foo() = flow {
    flowOf(false).transformLatest {
        emit(false)
    }
    emit(0)
}
