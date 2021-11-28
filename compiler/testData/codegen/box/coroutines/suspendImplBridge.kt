// TARGET_BACKEND: JVM
// WITH_STDLIB
// WITH_COROUTINES

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var result: String = ""
var proceed: (() -> Unit)? = null

fun box(): String {
    async {
        O().foo(1)
        O().foo(2)
    }
    while (proceed != null) {
        result += "--;"
        proceed!!()
    }
    if (result != "begin(1);--;end(1);begin(2);--;end(2);done;") return result;
    return "OK"
}

open class O {
    open suspend fun foo(x: Int) {
        result += "begin($x);"
        sleep()
        result += "end($x);"
    }
}

suspend fun sleep(): Unit = suspendCoroutine { c ->
    proceed = { c.resume(Unit) }
}

fun async(f: suspend () -> Unit) {
    f.startCoroutine(object : Continuation<Unit> {
        override fun resumeWith(x: Result<Unit>) {
            result += "done;"
            proceed = null
        }
        override val context = EmptyCoroutineContext
    })
}
