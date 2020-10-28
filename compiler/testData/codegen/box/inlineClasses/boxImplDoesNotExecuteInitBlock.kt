// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: JS_IR
// IGNORE_LIGHT_ANALYSIS

inline class IC(val i: Int) {
    init {
        counter += i
    }
}

var counter = 0

fun <T> id(t: T) = t

fun box(): String {
    val ic = IC(42)
    if (counter != 42) return "FAIL 1: $counter"
    counter = 0

    id(ic)
    if (counter != 0) return "FAIL 2: $counter"

    return "OK"
}