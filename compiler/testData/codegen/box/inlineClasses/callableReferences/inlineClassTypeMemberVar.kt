// WITH_STDLIB
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val x: Int)

class C(var z: Z)

fun box(): String {
    val ref = C::z

    val x = C(Z(42))

    if (ref.get(x).x != 42) throw AssertionError()

    ref.set(x, Z(1234))
    if (ref.get(x).x != 1234) throw AssertionError()

    return "OK"
}