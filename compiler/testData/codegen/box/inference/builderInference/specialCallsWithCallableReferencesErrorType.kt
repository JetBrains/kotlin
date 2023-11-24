// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// FIR status: Disabling of StrictOnlyInputTypesChecks is not supported by FIR
// WITH_STDLIB
// !LANGUAGE: -StrictOnlyInputTypesChecks

import kotlin.experimental.ExperimentalTypeInference

fun <K> FlowCollector<K>.bar(): K = null as K
fun <K> FlowCollector<K>.foo(): K = null as K

fun bar2(): Int = 1
fun foo2(): Float = 1f

fun <T> materialize() = null as T

interface FlowCollector<in T> {}

@Suppress("OPT_IN_USAGE_ERROR")
fun <L> flow(block: suspend FlowCollector<L>.() -> Unit) = Flow(block)

class Flow<out R>(private val block: suspend FlowCollector<R>.() -> Unit)

fun <R> select(vararg x: R) = x[0]

fun poll01(): Flow<String> {
    return flow {
        val inv = select(::bar2, ::foo2)
        inv()
    }
}


fun poll21(flag: Boolean): Flow<String> {
    return flow {
        val inv = when (flag) { true -> ::bar2 else -> ::foo2 }
        inv()
    }
}

fun poll31(flag: Boolean): Flow<String> {
    return flow {
        val inv = when (flag) { true -> ::bar2 false -> ::foo2 }
        inv()
    }
}

fun poll61(): Flow<String> {
    return flow {
        val inv = ::bar2
        inv
    }
}

fun poll71(): Flow<String> {
    return flow {
        val inv = ::bar2!!
        inv()
    }
}

fun poll81(): Flow<String> {
    return flow {
        val inv = ::bar2 in setOf(::foo2)
        inv
    }
}

fun poll91(): Flow<String> {
    return flow {
        val inv = ::foo2 in setOf(::foo2)
        inv
    }
}

fun box(): String {
    poll01()
    poll21(true)
    poll31(true)
    poll61()
    poll71()
    poll81()
    poll91()
    return "OK"
}
