// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

fun Long.inLongs(l: Long, r: Long): Boolean {
    return this in l..r
}

fun Double.inDoubles(l: Double, r: Double): Boolean {
    return this in l..r
}

fun box(): String {
    assert(2L.inLongs(1L, 3L))
    assert(!2L.inLongs(0L, 1L))

    assert(2.0.inDoubles(1.0, 3.0))
    assert(!2.0.inDoubles(0.0, 1.0))

    return "OK"
}
