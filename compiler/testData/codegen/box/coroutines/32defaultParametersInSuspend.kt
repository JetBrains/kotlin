// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    suspend fun suspendHere(
            a: String = "abc",
            i1: Int = 1,
            i2: Int = 1,
            i3: Int = 1,
            i4: Int = 1,
            i5: Int = 1,
            i6: Int = 1,
            i7: Int = 1,
            i8: Int = 1,
            i9: Int = 1,
            i10: Int = 1,
            i11: Int = 1,
            i12: Int = 1,
            i13: Int = 1,
            i14: Int = 1,
            i15: Int = 1,
            i16: Int = 1,
            i17: Int = 1,
            i18: Int = 1,
            i19: Int = 1,
            i20: Int = 1,
            i21: Int = 1,
            i22: Int = 1,
            i23: Int = 1,
            i24: Int = 1,
            i25: Int = 1,
            i26: Int = 1,
            i27: Int = 1,
            i28: Int = 1,
            i29: Int = 1,
            i30: Int = 1,
            i31: Int = 1
    ): String = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(a + "#" + (i1 + i2 + i3 + i4 + i5 + i6 + i7 + i8 + i9 + i10 + i11 + i12 + i13 + i14 + i15 + i16 + i17 + i18 + i19 + i20 + i21 + i22 + i23 + i24 + i25 + i26 + i27 + i28 + i29 + i30 + i31))
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = "OK"

    builder {
        var a = suspendHere()
        if (a != "abc#31") {
            result = "fail 1: $a"
            throw RuntimeException(result)
        }

        a = suspendHere("cde")
        if (a != "cde#31") {
            result = "fail 2: $a"
            throw RuntimeException(result)
        }

        a = suspendHere(i2 = 6)
        if (a != "abc#36") {
            result = "fail 3: $a"
            throw RuntimeException(result)
        }

        a = suspendHere("xyz", 9)
        if (a != "xyz#39") {
            result = "fail 4: $a"
            throw RuntimeException(result)
        }
    }

    return result
}
