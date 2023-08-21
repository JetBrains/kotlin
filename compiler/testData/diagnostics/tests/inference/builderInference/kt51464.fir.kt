// !RENDER_DIAGNOSTICS_FULL_TEXT
interface FlowCollector<in T> {}

interface Flow<out T>

fun <X, Y> Flow<X>.transform(transform: FlowCollector<Y>.(X) -> Unit): Flow<Y> = TODO()

fun f(x: Flow<Int>) {
    fun <T> doEmit(collector: FlowCollector<T>) {}
    x.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>transform<!> { doEmit(this) }
}
