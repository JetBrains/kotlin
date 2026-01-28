// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

@JvmInline
value class IC(val s: String?)

fun foo(ic: IC) {
    if (ic.s != "OK") throw AssertionError("Fail: ${ic.s}")
}

inline fun withDefaultIC(ic: IC = IC("OK")) {
    foo(ic)
}

fun box(): String {
    withDefaultIC()
    return "OK"
}
