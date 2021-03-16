// FIR_IDENTICAL
package jet121

fun box(): String {
    val answer = apply("OK") {
        get(0)
        length
    }

    return if (answer == 2) "OK" else "FAIL"
}

fun apply(arg: String, f: String.() -> Int): Int {
    return arg.f()
}
