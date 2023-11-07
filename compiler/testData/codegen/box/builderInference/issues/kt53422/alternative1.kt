// ISSUE: KT-53422

class Buildee<T>

fun interface FlowCollector<in T> {
    fun emit(value: T)
}

class Flow<out T> {
    fun collect(action: FlowCollector<T>) {}
}

fun <T> execute(
    value: T,
    lambda: Buildee<T>.(T) -> Unit
) {
    Buildee<T>().apply { lambda(value) }
}

fun <T> flow(block: FlowCollector<T>.() -> Unit): Flow<T> = Flow()

class TargetType

@Suppress("BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION")
fun box(): String {
    execute(
        flow { emit(TargetType()) },
        // K1&K2: UNRESOLVED_REFERENCE
        { it.collect { } }
    )
    return "OK"
}
