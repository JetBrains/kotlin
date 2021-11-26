// IGNORE_BACKEND: NATIVE
// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*


fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun expectsLambdaWithBigArity(c: suspend (Long, Long, Long, Long, Long, Long, Long, Long, Long, Long,
                                                  Long, Long, Long, Long, Long, Long, Long, Long, Long, Long,
                                                  Long, Long, Long, Long, Long, Long, Long, Long, Long, String) -> String): String {
    return c.invoke(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, "OK")
}

// Uses Function22.invoke with individual arguments. Function22 and not Function21 because of the added continuation parameter.
suspend fun expectsLambdaWithArity21(c: suspend (Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
                                                 Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
                                                 String) -> String): String {
    return c.invoke(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, "OK")
}

// Uses FunctionN.invoke with varargs array of arguments.
suspend fun expectsLambdaWithArity22(c: suspend (Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
                                                 Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
                                                 Int, String) -> String): String {
    return c.invoke(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, "OK")
}

fun box(): String {
    var res = "FAIL 1"
    builder {
        res = expectsLambdaWithBigArity { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, s -> s }
    }
    if (res != "OK") return res
    res = "FAIL 2"
    builder {
        res = expectsLambdaWithArity21 {
            i1, i2, i3, i4, i5, i6, i7, i8, i9, i10, i11, i12, i13, i14, i15, i16, i17, i18, i19, i20, s -> s
        }
    }
    if (res != "OK") return res
    res = "FAIL 3"
    builder {
        res = expectsLambdaWithArity22 {
            i1, i2, i3, i4, i5, i6, i7, i8, i9, i10, i11, i12, i13, i14, i15, i16, i17, i18, i19, i20, i21, s -> s
        }
    }
    return res
}
