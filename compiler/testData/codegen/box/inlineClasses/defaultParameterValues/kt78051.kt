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

// From original bug report
inline fun withDefaultICAndCrossinline(
    ic: IC = IC("OK"),
    crossinline check: (IC) -> Unit
) {
    check(ic)
}

fun box(): String {
    withDefaultIC()

    withDefaultICAndCrossinline { ic ->
        if (ic.s != "OK") throw AssertionError("Fail crossinline: ${ic.s}")
    }

    return "OK"
}
