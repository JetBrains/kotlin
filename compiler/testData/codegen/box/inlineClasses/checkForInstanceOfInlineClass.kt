// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class UInt(val u: Int) {
    override fun toString(): String {
        return "UInt: $u"
    }
}

fun Any.isUInt(): Boolean = this is UInt
fun Any.notIsUInt(): Boolean = this !is UInt

inline fun <reified T> Any?.instanceOf(): Boolean = this is T

fun UInt.extension(): String = "OK:"

fun foo(x: UInt?): String {
    if (x is UInt) {
        return x.extension() + x.toString()
    }

    return "fail"
}

fun bar(x: UInt?): String {
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


    if (!u.instanceOf<UInt>()) return "fail"
    if (1.instanceOf<UInt>()) return "fail"

    val nullableUInt: UInt? = UInt(10)
    if (!nullableUInt.instanceOf<UInt>()) return "fail"

    val nullAsUInt: UInt? = null
    if (nullAsUInt.instanceOf<UInt>()) return "fail"
    if (!nullAsUInt.instanceOf<UInt?>()) return "fail"

    if (foo(u) != "OK:UInt: 12") return "fail"
    if (bar(u) != "OK:") return "fail"

    return "OK"
}