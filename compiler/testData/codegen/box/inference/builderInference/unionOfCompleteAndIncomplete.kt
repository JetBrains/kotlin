interface Flow<T> {
    fun collect(collector: FlowCollector<T>)
}

interface FlowCollector<T> {
    fun emit(value: T)
}

fun <R> flow(block: FlowCollector<R>.() -> Unit): Flow<R> =
    object : Flow<R> {
        override fun collect(collector: FlowCollector<R>) =
            collector.block()
    }

sealed class Outcome<out V> {
    abstract val value: V
}

class Success<out U>(override val value: U) : Outcome<U>()

class Failure<out U>() : Outcome<U>() {
    override val value: U
        get() = TODO()
}

class Complete<R>(val outcome: Outcome<R>)

fun <X, Y> foo(it: X, block: (X) -> Y) =
    flow {
        emit(Complete(if (true) Success(block(it)) else Failure()))
    }

fun box(): String {
    var result = "fail"
    foo("O") { it + "K" }.collect(object : FlowCollector<Complete<String>> {
        override fun emit(value: Complete<String>) {
            result = value.outcome.value
        }
    })
    return result
}
