// !LANGUAGE: +ReleaseCoroutines
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun id(s: String) = s

suspend fun String.idExt() = this

class A {
    suspend fun id(s: String) = s
}

suspend fun run(block: suspend () -> String) = block()

inline suspend fun runInline(block: suspend () -> String) = block()

suspend fun O(block: suspend (String) -> String) = block("O")

inline suspend fun K(block: suspend (String) -> String) = block("K")

suspend fun ok(o: String): String {
    return o + run {
        if (o != "K") {
            (::ok)("K")
        } else ""
    }
}

fun box(): String {
    var result = ""

    builder {
        result = O(::id) + K(::id)
        result += run("O"::idExt) + runInline("K"::idExt)

        val a = A()
        result += O(a::id) + K(a::id)

        result += ok("O")
    }

    if (result != "OKOKOKOK") return "fail: $result"

    return "OK"
}
