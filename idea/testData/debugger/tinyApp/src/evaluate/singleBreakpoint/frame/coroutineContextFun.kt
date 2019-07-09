package coroutineContextFun

import kotlin.coroutines.coroutineContext

suspend fun main() {
    val a = 5
    foo()
    //Breakpoint!
    foo()
}

private suspend fun foo() {}

// EXPRESSION: coroutineContext
// RESULT: instance of kotlin.coroutines.EmptyCoroutineContext(id=ID): Lkotlin/coroutines/EmptyCoroutineContext;