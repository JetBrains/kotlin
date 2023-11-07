// ISSUE: KT-53422

class Buildee<T>

fun interface FlowCollector<in T> {
    fun emit(value: T)
}

class Flow<out T>
fun <T> Flow<T>.collect(action: (T) -> Unit) {}

fun <T> execute(
    value: T,
    lambda: Buildee<T>.(T) -> Unit
) {
    Buildee<T>().apply { lambda(value) }
}

fun <T> flow(block: FlowCollector<T>.() -> Unit): Flow<T> = Flow()

class TargetType

@Suppress("BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION", "BUILDER_INFERENCE_STUB_RECEIVER")
fun box(): String {
    execute(
        flow { emit(TargetType()) },
        // K1&K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
        { it.collect { } }
    )
    return "OK"
}
