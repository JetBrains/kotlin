// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt<T: Int>(val u: T) {
    override fun toString(): String {
        return "UInt: $u"
    }
}

fun Any.isUInt(): Boolean = this is UInt<*>
fun Any.notIsUInt(): Boolean = this !is UInt<*>

inline fun <reified T> Any?.instanceOf(): Boolean = this is T

fun UInt<Int>.extension(): String = "OK:"

fun foo(x: UInt<Int>?): String {
    if (x is UInt<*>) {
        return x.extension() + x.toString()
    }

    return "fail"
}

fun bar(x: UInt<Int>?): String {
    if (x is Any) {
        return x.extension()
    }

    return "fail"
}

fun box(): String {
    val u = UInt(12)
    if (!u.isUInt()) return "fail"
    if (u.notIsUInt()) return "fail"

    if (1.isUInt()) return "fail"
    if (!1.notIsUInt()) return "fail"


    if (!u.instanceOf<UInt<Int>>()) return "fail"
    if (1.instanceOf<UInt<Int>>()) return "fail"

    val nullableUInt: UInt<Int>? = UInt(10)
    if (!nullableUInt.instanceOf<UInt<Int>>()) return "fail"

    val nullAsUInt: UInt<Int>? = null
    if (nullAsUInt.instanceOf<UInt<Int>>()) return "fail"
    if (!nullAsUInt.instanceOf<UInt<Int>?>()) return "fail"

    if (foo(u) != "OK:UInt: 12") return "fail"
    if (bar(u) != "OK:") return "fail"

    return "OK"
}