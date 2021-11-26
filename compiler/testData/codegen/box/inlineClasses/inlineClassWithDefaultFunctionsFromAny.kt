// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val data: Int)

fun box(): String {
    if (Z(0) != Z(0)) throw AssertionError()
    if (Z(0) == Z(1)) throw AssertionError()

    if (Z(1234).hashCode() != 1234) throw AssertionError(Z(1234).hashCode().toString())

    if (Z(0).toString() != "Z(data=0)") throw AssertionError(Z(0).toString())

    return "OK"
}