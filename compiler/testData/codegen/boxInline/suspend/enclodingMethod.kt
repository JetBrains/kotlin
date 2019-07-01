// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: flow.kt
// COMMON_COROUTINES_TEST
// FULL_JDK
// WITH_RUNTIME
// WITH_COROUTINES

package flow

interface FlowCollector<T> {
    suspend fun emit(value: T)
}

interface Flow<T> {
    suspend fun collect(collector: FlowCollector<T>)
}

public inline fun <T> flow(crossinline block: suspend FlowCollector<T>.() -> Unit) = object : Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) = collector.block()
}

suspend inline fun <T> Flow<T>.collect(crossinline action: suspend (T) -> Unit): Unit =
    collect(object : FlowCollector<T> {
        override suspend fun emit(value: T) = action(value)
    })

public inline fun <T, R> Flow<T>.transform(crossinline transformer: suspend FlowCollector<R>.(value: T) -> Unit): Flow<R> {
    return flow {
        collect { value ->
            transformer(value)
        }
    }
}

public inline fun <T, R> Flow<T>.map(crossinline transformer: suspend (value: T) -> R): Flow<R> = transform { value -> emit(transformer(value)) }

inline fun decorate() = suspend {
    flow<Int> {
        emit(1)
    }.map { it + 1 }
        .collect {
        }
}

// FILE: box.kt
// COMMON_COROUTINES_TEST

import flow.*

fun box() : String {
    decorate()
    val enclosingMethod = try {
        Class.forName("flow.FlowKt\$decorate\$1\$invokeSuspend\$\$inlined\$map\$1\$1").enclosingMethod
    } catch (ignore: ClassNotFoundException) {
        Class.forName("flow.FlowKt\$decorate\$1\$doResume\$\$inlined\$map\$1\$1").enclosingMethod
    }
    if ("$enclosingMethod".contains("\$\$forInline")) return "$enclosingMethod"
    return "OK"
}
