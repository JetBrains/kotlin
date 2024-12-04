// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt<T: Int>(private val value: T) {
    operator fun plus(other: UInt<Int>): UInt<Int> = UInt<Int>(value + other.asValue())

    fun asValue(): Int = value
}

val Int.u get() = UInt(this)

var global = 0.u

fun testInlined(x: UInt<Int>?, withAssert: Boolean) {
    x?.myLet {
        takeUInt(it)
        takeUInt(x)
    }

    x?.myLet {
        takeNullableUInt(it)
        takeNullableUInt(x)
    }

    if (withAssert) {
        x!!.myLet {
            takeUInt(it)
            takeUInt(x)
        }
    }
}

fun testNotInlined(x: UInt<Int>?) {
    x?.myLet {
        takeUInt(it)
        takeUInt(x)
    }

    x?.myLet {
        takeNullableUInt(it)
        takeNullableUInt(x)
    }
}

fun takeUInt(y: UInt<Int>) {
    global += y
}

fun takeNullableUInt(y: UInt<Int>?) {
    if (y != null) {
        global += y
    }
}

inline fun <T> T.myLet(f: (T) -> Unit) = f(this)
fun <T> T.nonInlineLet(f: (T) -> Unit) = f(this)

fun box(): String {
    val u = 1.u
    testInlined(u, true)
    if (global.asValue() != 6) return "fail 1"

    global = 0.u
    testInlined(null, false)
    if (global.asValue() != 0) return "fail 2"

    global = 0.u
    testNotInlined(u)
    if (global.asValue() != 4) return "fail 3"

    return "OK"
}