// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

@JvmInline
value class ICNullableString(val s: String?)

@JvmInline
value class ICNullableInt(val x: Int?)

@JvmInline
value class ICString(val s: String)

@JvmInline
value class ICInt(val x: Int)

fun fooNullableString(ic: ICNullableString) {
    if (ic.s != "OK") throw AssertionError("Fail NullableString: ${ic.s}")
}

fun fooNullableInt(ic: ICNullableInt) {
    if (ic.x != 42) throw AssertionError("Fail NullableInt: ${ic.x}")
}

fun fooString(ic: ICString) {
    if (ic.s != "OK") throw AssertionError("Fail String: ${ic.s}")
}

fun fooInt(ic: ICInt) {
    if (ic.x != 42) throw AssertionError("Fail Int: ${ic.x}")
}

inline fun withDefaultICNullableString(ic: ICNullableString = ICNullableString("OK")) {
    fooNullableString(ic)
}

inline fun withDefaultICNullableInt(ic: ICNullableInt = ICNullableInt(42)) {
    fooNullableInt(ic)
}

inline fun withDefaultICString(ic: ICString = ICString("OK")) {
    fooString(ic)
}

inline fun withDefaultICInt(ic: ICInt = ICInt(42)) {
    fooInt(ic)
}

// From the original bug report
inline fun withDefaultICAndCrossinline(
    ic: ICNullableString = ICNullableString("OK"),
    crossinline check: (ICNullableString) -> Unit
) {
    check(ic)
}

fun box(): String {
    withDefaultICNullableString()
    withDefaultICNullableInt()
    withDefaultICString()
    withDefaultICInt()

    withDefaultICAndCrossinline { ic ->
        if (ic.s != "OK") throw AssertionError("Fail crossinline: ${ic.s}")
    }

    return "OK"
}
