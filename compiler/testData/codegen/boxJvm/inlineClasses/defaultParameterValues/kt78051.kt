// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// IGNORE_DEXING

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

// Class member with dispatch receiver — matches original bug report structure.
// Dispatch receiver sits at parameters[0], shifting value class param indices.
class Host {
    private inline fun privateMember(
        saved: ICNullableString = ICNullableString("OK"),
        crossinline check: (ICNullableString) -> Unit
    ) {
        check(saved)
    }

    fun run(): String {
        var result = "FAIL"
        privateMember { ic ->
            if (ic.s != "OK") throw AssertionError("Fail private member: ${ic.s}")
            result = "OK"
        }
        return result
    }
}

// Nullable IC outer — both stub and impl should agree on JVM type (fast path preserved)
inline fun nullableOuter(ic: ICNullableString? = null) {
    if (ic != null) throw AssertionError("Fail nullable outer: expected null")
}

fun box(): String {
    withDefaultICNullableString()
    withDefaultICNullableInt()
    withDefaultICString()
    withDefaultICInt()

    withDefaultICAndCrossinline { ic ->
        if (ic.s != "OK") throw AssertionError("Fail crossinline: ${ic.s}")
    }

    if (Host().run() != "OK") return "FAIL: private member"

    nullableOuter()

    return "OK"
}
