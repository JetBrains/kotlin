// WITH_RUNTIME

@JvmInline
value class Z(val x: Int)
@JvmInline
value class L(val x: Long)
@JvmInline
value class S(val x: String)

fun Z.test() = x
fun L.test() = x
fun S.test() = x

fun box(): String {
    if (Z::test.invoke(Z(42)) != 42) throw AssertionError()
    if (L::test.invoke(L(1234L)) != 1234L) throw AssertionError()
    if (S::test.invoke(S("abcdef")) != "abcdef") throw AssertionError()

    return "OK"
}