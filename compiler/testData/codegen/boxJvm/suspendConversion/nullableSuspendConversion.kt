// TARGET_BACKEND: JVM
// WITH_STDLIB
// ISSUE: KT-89504
import kotlin.coroutines.*

class Bean { fun computeInt(): Int = 42 }

fun sink(f: (suspend () -> Int)?): String {
    if (f == null) return "null"
    var r: Any? = "none"
    f.startCoroutine(object : Continuation<Int> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Int>) { r = result.getOrNull() }
    })
    return if (r == 42) "OK" else "$r"
}

fun box(): String {
    val b: Bean? = Bean()
    return sink(if (b == null) null else b::computeInt)
}
