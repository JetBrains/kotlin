// KJS_WITH_FULL_RUNTIME
// JVM_TARGET: 1.6
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR

val ua = 1234UL
val ub = 5678UL

fun box(): String {
    if (ua.compareTo(ub) > 0) {
        throw AssertionError()
    }

    return "OK"
}
