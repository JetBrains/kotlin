// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
fun <T> flowOf(value: T): Flow<T> = TODO()

interface FlowCollector<in T> {}

interface Flow<out T>

fun <T, R> Flow<T>.transform(transform: FlowCollector<R>.(T) -> Unit): Flow<R> = TODO()

fun f() {
    fun <T> doEmit(collector: FlowCollector<T>) {}
    flowOf(1).<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>transform<!> { <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>doEmit<!>(this) }
}
