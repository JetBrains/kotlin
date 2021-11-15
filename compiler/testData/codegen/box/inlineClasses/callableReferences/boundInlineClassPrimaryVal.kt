// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val x: Int)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class L(val x: Long)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class S(val x: String)

fun box(): String {
    if (Z(42)::x.get() != 42) throw AssertionError()
    if (L(1234L)::x.get() != 1234L) throw AssertionError()
    if (S("abcdef")::x.get() != "abcdef") throw AssertionError()

    return "OK"
}