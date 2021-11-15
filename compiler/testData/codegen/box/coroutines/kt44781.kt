// WITH_STDLIB

import kotlin.coroutines.*

inline suspend fun foo(crossinline block: () -> String): String {
    return bar { _ -> block() }
}

suspend fun bar(block: suspend (Int) -> String): String {
    return block(1)
}

fun launch(block: suspend () -> String): String {
    var result = ""
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it.getOrThrow() })
    return result
}

fun box(): String {
    return launch {
        foo {
            "OK"
        }
    }
}
