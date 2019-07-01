package stringUpdateVariable

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

fun main(args: Array<String>) {
    builder {
        var s = "aabb"
        s = strChanger(s)
        //Breakpoint!
        println(s) // (1)
    }
}

suspend fun strChanger(str: String): String = str.filter { it !in "a" }

// EXPRESSION: s
// RESULT: "bb": Ljava/lang/String;
