// IGNORE_BACKEND_FIR: JVM_IR
// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

fun <R> foo(f: () -> R): R = f()

fun <T : Any> some(v: T?, b: T): T {
    return foo {
        if (v != null) {
            v
        }
        else {
            b
        }
    }
}

fun <T : Any> some1(v: T?, b: T): T {
    return foo { if (v != null) v else b }
}

fun box() = when {
    some(1, 2) != 1 -> "fail 1"
    some(null, 2) != 2 -> "fail 2"
    some1(1, 2) != 1 -> "fail 3"
    else -> "OK"
}
