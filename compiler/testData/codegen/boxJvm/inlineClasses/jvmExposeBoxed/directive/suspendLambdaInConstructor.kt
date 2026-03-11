// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ImplicitJvmExposeBoxed

// FILE: IC.kt
import kotlin.coroutines.*

@JvmInline
value class E2Test1(val x: suspend () -> Unit)

data class E2Test2(val y: E2Test1 = E2Test1( { result = "OK" } ))

var result = "FAIL"

fun box(): String {
    val e2test2 = E2Test2()
    e2test2.y.x.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
    return "OK"
}
