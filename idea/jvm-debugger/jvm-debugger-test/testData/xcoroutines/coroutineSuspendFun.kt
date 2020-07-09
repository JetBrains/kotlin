package continuation
// ATTACH_LIBRARY: maven(org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.3.8)-javaagent

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

fun main() {
    val main = "main"
    runBlocking {
        a()
    }
}

suspend fun a() {
    val a = "a"
    b(a)
    val aLate = "a" // to prevent stackFrame to collapse
}

suspend fun b(paramA: String) {
    yield()
    val b = "b"
    c(b)
}

suspend fun c(paramB: String) {
    val c = "c"
    //Breakpoint!
}