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
    if (42.let(::Z).x != 42) throw AssertionError()
    if (1234L.let(::L).x != 1234L) throw AssertionError()
    if ("abcdef".let(::S).x != "abcdef") throw AssertionError()

    return "OK"
}