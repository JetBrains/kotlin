// WITH_STDLIB
// WITH_COROUTINES

// MODULE: lib
// FILE: lib.kt

class AAA
class ZZZ

open class A {
    // function signatures are chosen in a way, that their order for Native will change, if suspend is not lowered correctly
    open suspend fun f(x:Int) = 1
    open fun f(x:AAA) = 2
    open fun f(x:ZZZ) = 3
}

// MODULE: main(lib)
// FILE: main.kt

import kotlin.coroutines.*
import helpers.*


class ImplementA : A(){
    override suspend fun f(x:Int) = 4
}

class NotImplementA : A() {}


fun runF(x: A, f: suspend A.() -> Int) : Int {
    var res: Int = -1
    f.startCoroutine(x, handleResultContinuation {
        res = it
    })
    return res
}

fun box() : String {
    val x = if (true) ImplementA() else A() // to avoid devirtualization
    if (x.f(AAA()) != 2) return "FAIL 1"
    if (x.f(ZZZ()) != 3) return "FAIL 2"
    if (runF(x, { f(1) }) != 4) return "FAIL 3"
    val y = if (true) NotImplementA() else A() // to avoid devirtualization
    if (y.f(AAA()) != 2) return "FAIL 4"
    if (y.f(ZZZ()) != 3) return "FAIL 5"
    if (runF(y, { f(1) }) != 1) return "FAIL 6"
    return "OK"
}
