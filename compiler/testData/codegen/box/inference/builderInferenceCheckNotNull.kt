// !LANGUAGE: +NewInference
// WITH_RUNTIME
import kotlin.experimental.ExperimentalTypeInference

interface Flow<out T> {
    fun collect(collector: FlowCollector<T>)
}

interface FlowCollector<in T> {
    fun emit(value: T)
}

fun <T> flow(block: FlowCollector<T>.() -> Unit): Flow<T> =
    object : Flow<T> {
        override fun collect(collector: FlowCollector<T>) = collector.block()
    }

fun <T> Flow<T>.collect(action: (value: T) -> Unit): Unit =
    collect(object : FlowCollector<T> {
        override fun emit(value: T) = action(value)
    })

@OptIn(ExperimentalTypeInference::class)
fun <T, R> Flow<T>.transform(@BuilderInference transform: FlowCollector<R>.(T) -> Unit): Flow<R> =
    flow { collect { transform(it) } }

// Due to the BuilderInference, the data flow info for the expected type of the argument to
// emit contains an unsolved type variable, while the type of `transform(it)` is `R`.
// Since unsolved type variables have no upper bounds, the argument to emit is assumed to
// be non-nullable, while `R` is nullable and we could insert a checkNotNullExpressionValue
// unless we deal with unsolved type parameters in `RuntimeAssertions.kt`.
fun <T, R> Flow<T>.map(transform: (value: T) -> R): Flow<R> =
    transform { emit(transform(it)) }

fun box(): String {
    var result: String = "Fail 1"
    flow<String> { emit("Fail 2") }.map { null }.collect { result = it ?: "OK" }
    return result
}
