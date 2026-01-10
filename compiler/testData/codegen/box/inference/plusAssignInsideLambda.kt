// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB

// FILE: lib.kt
import kotlin.experimental.ExperimentalTypeInference

interface SendChannel<in T> {
    suspend fun send(value: T)
}

@OptIn(ExperimentalTypeInference::class)
public fun <T> flux(block: suspend SendChannel<T>.() -> Unit) {}

suspend inline fun <T> T.collect(action: (T) -> Unit) { action(this) }

// FILE: main.kt
fun test() {
    flux {
        var result = ""
        "OK".collect { result += it }
        send(result)
    }
}

fun box() = "OK"