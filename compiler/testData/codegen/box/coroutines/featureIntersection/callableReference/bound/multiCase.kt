// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class A(var v: Int) {
    suspend fun f(x: Int) = x * v
}

suspend fun A.g(x: Int) = x * f(x);

var A.w: Int
    get() = 1000 * v
    set(c: Int) {
        v = c + 10
    }

object F {
    var u = 0
}

fun box(): String {
    builder {
        val a = A(5)

        val av = a::v
        if (av() != 5) throw RuntimeException("fail1: ${av()}")
        if (av.get() != 5) throw RuntimeException("fail2: ${av.get()}")
        av.set(7)
        if (a.v != 7) throw RuntimeException("fail3: ${a.v}")

        val af = a::f
        if (af(10) != 70) throw RuntimeException("fail4: ${af(10)}")

        val ag = a::g
        if (ag(10) != 700) throw RuntimeException("fail5: ${ag(10)}")

        val aw = a::w
        if (aw() != 7000) throw RuntimeException("fail6: ${aw()}")
        if (aw.get() != 7000) throw RuntimeException("fail7: ${aw.get()}")
        aw.set(5)
        if (a.v != 15) throw RuntimeException("fail8: ${a.v}")

        val fu = F::u
        if (fu() != 0) throw RuntimeException("fail9: ${fu()}")
        if (fu.get() != 0) throw RuntimeException("fail10: ${fu.get()}")
        fu.set(8)
        if (F.u != 8) throw RuntimeException("fail11: ${F.u}")

        val x = 100

        fun A.lf() = v * x;
        val alf = a::lf
        if (alf() != 1500) throw RuntimeException("fail9: ${alf()}")
    }

    return "OK"
}
