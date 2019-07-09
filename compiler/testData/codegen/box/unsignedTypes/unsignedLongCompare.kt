// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val ua = 1234UL
val ub = 5678UL

fun box(): String {
    if (ua.compareTo(ub) > 0) {
        throw AssertionError()
    }

    return "OK"
}
