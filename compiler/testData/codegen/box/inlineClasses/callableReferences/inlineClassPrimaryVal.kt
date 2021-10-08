// WITH_RUNTIME

@JvmInline
value class Z(val x: Int)
@JvmInline
value class L(val x: Long)
@JvmInline
value class S(val x: String)

fun box(): String {
    if ((Z::x).get(Z(42)) != 42) throw AssertionError()
    if ((L::x).get(L(1234L)) != 1234L) throw AssertionError()
    if ((S::x).get(S("abcdef")) != "abcdef") throw AssertionError()

    return "OK"
}