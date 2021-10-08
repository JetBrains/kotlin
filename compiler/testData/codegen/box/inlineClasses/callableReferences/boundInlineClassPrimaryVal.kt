// WITH_RUNTIME

@JvmInline
value class Z(val x: Int)
@JvmInline
value class L(val x: Long)
@JvmInline
value class S(val x: String)

fun box(): String {
    if (Z(42)::x.get() != 42) throw AssertionError()
    if (L(1234L)::x.get() != 1234L) throw AssertionError()
    if (S("abcdef")::x.get() != "abcdef") throw AssertionError()

    return "OK"
}