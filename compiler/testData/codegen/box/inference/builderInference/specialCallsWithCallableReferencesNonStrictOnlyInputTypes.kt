// WITH_STDLIB
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: WASM

import kotlin.experimental.ExperimentalTypeInference


@OptIn(ExperimentalTypeInference::class)
fun <R> scopedFlow(@BuilderInference block: suspend CoroutineScope.(FlowCollector<R>) -> Unit): Flow<R> =
    flow {
        val collector = this
        flowScope { block(collector) }
    }

@OptIn(ExperimentalTypeInference::class)
fun <T> flow(@BuilderInference block: suspend FlowCollector<T>.() -> Unit): Flow<T> = TODO()

@OptIn(ExperimentalTypeInference::class)
fun <R> flowScope(@BuilderInference block: suspend CoroutineScope.() -> R): R = TODO()

interface CoroutineScope
interface Flow<out T>

interface FlowCollector<in T>

fun box() = "OK"
