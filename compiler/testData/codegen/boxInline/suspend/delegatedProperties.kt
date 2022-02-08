// WITH_REFLECT
// WITH_COROUTINES
// WITH_STDLIB
// FILE: test.kt

import kotlin.coroutines.*
import helpers.*
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return "OK"
    }
}

interface SuspendRunnable {
    suspend fun run(): String
}

suspend inline fun test(crossinline c: suspend (String) -> String): String {
    val sr = object : SuspendRunnable {
        val ok by Delegate()
        override suspend fun run(): String {
            return c(ok)
        }
    }
    return sr.run()
}

// FILE: box.kt
import kotlin.coroutines.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun dummy() {}

fun box(): String {
    var res: String = "FAIL"
    builder {
        res = test { it }
    }
    return res
}
