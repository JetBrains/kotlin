// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT
// WITH_COROUTINES


import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class A {
    companion {
        fun foo() = suspend { "O" }
    }
}
suspend companion fun A.bar() = { "K" }

fun box(): String {
    var res: String = ""
    builder {
        res =  A.foo()() + A.bar()()
    }
    return res
}
