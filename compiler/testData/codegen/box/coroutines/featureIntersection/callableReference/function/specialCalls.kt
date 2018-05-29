// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun baz(i: Int) = i
suspend fun <T> bar(x: T): T = x

suspend fun nullableFun(): (suspend (Int) -> Int)? = null

fun box(): String {
    builder {
      val x1: suspend (Int) -> Int = bar(if (true) ::baz else ::baz)
      val x2: suspend (Int) -> Int = bar(nullableFun() ?: ::baz)
      val x3: suspend (Int) -> Int = bar(::baz ?: ::baz)

      val i = 0
      val x4: suspend (Int) -> Int = bar(when (i) {
                                     10 -> ::baz
                                     20 -> ::baz
                                     else -> ::baz
                                 })

      val x5: suspend (Int) -> Int = bar(::baz!!)

      if (x1(1) != 1) throw RuntimeException("fail 1")
      if (x2(1) != 1) throw RuntimeException("fail 2")
      if (x3(1) != 1) throw RuntimeException("fail 3")
      if (x4(1) != 1) throw RuntimeException("fail 4")
      if (x5(1) != 1) throw RuntimeException("fail 5")

      if ((if (true) ::baz else ::baz)(1) != 1) throw RuntimeException("fail 6")
    }

    return "OK"
}