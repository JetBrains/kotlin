// WITH_RUNTIME
// DONT_TARGET_EXACT_BACKEND: WASM
// !LANGUAGE: -StrictOnlyInputTypesChecks

import kotlin.experimental.ExperimentalTypeInference

fun <K> FlowCollector<K>.bar(): K = null as K
fun <K> FlowCollector<K>.foo(): K = null as K

fun bar2(): Int = 1
fun foo2(): Float = 1f

val bar4: Int
    get() = 1

var foo4: Float
    get() = 1f
    set(value) {}

fun <T> materialize() = null as T

interface FlowCollector<in T> {}

@Suppress("EXPERIMENTAL_API_USAGE_ERROR")
fun <L> flow(@BuilderInference block: suspend FlowCollector<L>.() -> Unit) = Flow(block)

class Flow<out R>(private val block: suspend FlowCollector<R>.() -> Unit)

fun poll81(): Flow<String> {
    return flow {
        val inv = ::bar2 in setOf(::foo2)
        inv
    }
}

fun poll83(): Flow<String> {
    return flow {
        val inv = ::bar4 in setOf(::foo4)
        inv
    }
}

fun box(): String {
    poll81()
    poll83()
    return "OK"
}