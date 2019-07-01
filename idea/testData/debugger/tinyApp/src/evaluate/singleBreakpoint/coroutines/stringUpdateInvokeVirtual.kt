package stringUpdateInvokeVirtual

import kotlin.sequences.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit>{
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Unit>) {
            result.getOrThrow()
        }
    })
}

class A(var s: String)

fun main(args: Array<String>) {
    builder {
        var a = A("aabb")
        a.s = strChanger(a.s)
        //Breakpoint!
        println(a.s) // (1)
    }
}

suspend fun strChanger(str: String): String = str.filter { it !in "a" }

// EXPRESSION: a.s
// RESULT: "bb": Ljava/lang/String;