// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class UInt(val value: Int)

fun box(): String {
    val a = UInt(123)
    if(a.value != 123) return "fail"

    val c = a.value.hashCode()
    if (c.hashCode() != 123.hashCode()) return "fail"

    val b = UInt(100).value + a.value
    if (b != 223) return "faile"

    return "OK"
}