// WITH_RUNTIME

fun box(): String {
    for (i in (4 .. 1).reversed()) {
        throw AssertionError("Loop should not be executed")
    }
    for (i in (4L .. 1L).reversed()) {
        throw AssertionError("Loop should not be executed")
    }
    for (i in ('D' .. 'A').reversed()) {
        throw AssertionError("Loop should not be executed")
    }
    return "OK"
}

// 0 reversed
// 0 getFirst
// 0 getLast
// 0 getStep