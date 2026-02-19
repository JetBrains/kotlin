// KT-53629: K2: repeated labels on statements are not supported or diagnostic is incorrect
// IGNORE_BACKEND_K2: ANY
inline fun foo() = false

fun run(x: Boolean, y: Boolean): String {
    var z = 10
    l1@ l2@ do {
        z += 1
        if (z > 100) return "NOT_OK"
        if (x) continue@l1
        if (y) continue@l2
    } while(foo())

    return "OK"
}

fun box(): String {
    return run(true, true)
}
