// WITH_STDLIB

// FILE: 2.kt
package other
import builders.*
import kotlin.coroutines.*

fun test() {
    suspend {
        foo {  }
    }.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    test()
    return "OK"
}

// FILE: 1.kt
package builders

suspend fun foo(
    a: suspend () -> Unit = {}
) {}