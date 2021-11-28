// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val value: Int)

fun eq(a: Z?, b: Z) = a == b

fun eqq(a: Z, b: Z?) = a == b

fun box(): String {
    if (!eq(Z(1), Z(1))) throw AssertionError()
    if (eq(Z(1), Z(2))) throw AssertionError()
    if (eq(null, Z(0))) throw AssertionError()

    if (!eqq(Z(1), Z(1))) throw AssertionError()
    if (eqq(Z(1), Z(2))) throw AssertionError()
    if (eqq(Z(0), null)) throw AssertionError()

    return "OK"
}