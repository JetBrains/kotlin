// This test depends on line numbers
// WITH_STDLIB

// FILE: 1.kt
package test

interface SuspendRunnable {
    suspend fun run()
}

inline suspend fun inlineMe1(crossinline c: suspend () -> Unit): SuspendRunnable =
    object : SuspendRunnable {
        override suspend fun run() {
            c()
        }
    }

inline suspend fun inlineMe2(crossinline c: suspend () -> Unit): suspend () -> Unit =
    {
        c()
    }

// FILE: 2.kt

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import test.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            result.getOrThrow()
        }
    })
}

fun box(): String {
    var res = ""
    builder {
        inlineMe1 {
            res += "O"
        }.run()
        inlineMe2 {
            res += "K"
        }()
    }
    return res
}

