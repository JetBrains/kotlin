// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

fun check(x: Any?): Boolean {
    if (x is Int) {
        return x in 239..240
    }

    throw java.lang.AssertionError()
}

fun check(x: Any?, l: Any?, r: Any?): Boolean {
    if (x is Int && l is Int && r is Int) {
       return x in l..r
    }

    throw java.lang.AssertionError()
}


fun box(): String {
    assert(check(239))
    assert(check(239, 239, 240))
    assert(!check(238))
    assert(!check(238, 239, 240))
    return "OK"
}
