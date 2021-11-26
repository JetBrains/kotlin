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

val Z.xx get() = x
val L.xx get() = x
val S.xx get() = x

fun box(): String {
    if ((Z::xx).get(Z(42)) != 42) throw AssertionError()
    if ((L::xx).get(L(1234L)) != 1234L) throw AssertionError()
    if ((S::xx).get(S("abcdef")) != "abcdef") throw AssertionError()

    return "OK"
}