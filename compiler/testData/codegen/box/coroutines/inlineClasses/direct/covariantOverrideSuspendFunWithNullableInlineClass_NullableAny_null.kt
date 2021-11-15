// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@Suppress("UNSUPPORTED_FEATURE")
inline class IC(val s: Any?)

interface IBar {
    suspend fun bar(): IC?
}

class Test1() : IBar {
    override suspend fun bar(): IC {
        return IC(null)
    }

    suspend fun test(): Any? {
        val b: IBar = this
        return b.bar()!!.s
    }
}

class Test2() : IBar {
    override suspend fun bar(): IC {
        return IC(null)
    }

    suspend fun test(): IC {
        val b: IBar = this
        return b.bar()!!
    }
}


fun box(): String {
    var result: Any? = "FAIL 1"
    builder {
        result = Test1().test()
    }
    if (result != null) return "FAIL 1 $result"

    result = "FAIL 2"
    builder {
        result = Test2().test()
    }
    if (result != IC(null)) return "FAIL 2 $result"

    return "OK"
}
