// WITH_RUNTIME
// DONT_TARGET_EXACT_BACKEND: WASM

import kotlin.experimental.ExperimentalTypeInference

fun <K> FlowCollector<K>.bar(): K = null as K
fun <K> FlowCollector<K>.foo(): K = null as K

fun bar2(): Int = 1
fun foo2(): Float = 1f

fun <T> materialize() = null as T

interface FlowCollector<in T> {}

@Suppress("EXPERIMENTAL_API_USAGE_ERROR")
fun <L> flow(@BuilderInference block: suspend FlowCollector<L>.() -> Unit) = Flow(block)

class Flow<out R>(private val block: suspend FlowCollector<R>.() -> Unit)

fun poll1(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { ::bar2 } else { ::foo2 }
        inv()
    }
}

fun poll11(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { ::bar2 } else { ::foo2 }
        inv()
    }
}

fun poll41(): Flow<String> {
    return flow {
        val inv = try { ::bar2 } finally { ::foo2 }
        inv()
    }
}

fun poll51(): Flow<String> {
    return flow {
        val inv = try { ::bar2 } catch (e: Exception) { ::foo2 } finally { ::foo2 }
        inv()
    }
}

fun box(): String {
    poll1(true)
    poll11(true)
    poll41()
    poll51()
    return "OK"
}