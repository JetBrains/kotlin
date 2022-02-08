// WITH_STDLIB

import kotlin.coroutines.*

class B {
    val value: Long = 10L

    open inner class C : A<Unit> {
        override suspend fun getTotalFrames(): Long? = this@B.value

        open inner class D : A<Unit> {
            override suspend fun getTotalFrames(): Long? = this@B.value
        }

        suspend fun getInnerTotalFrames(): Long? = D().getTotalFrames()
    }

    suspend fun get1(): Long? {
        return C().getTotalFrames()
    }

    suspend fun get2(): Long? = C().getInnerTotalFrames()
}

interface A<T> {
    suspend fun getTotalFrames(): Long? = null
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = if (B().get1() == 10L) "OK" else "FAIL 1 ${B().get1()}"
    }
    if (res != "OK") return res

    res = "FAIL 2"
    builder {
        res = if (B().get2() == 10L) "OK" else "FAIL 2 ${B().get2()}"
    }
    return res
}
