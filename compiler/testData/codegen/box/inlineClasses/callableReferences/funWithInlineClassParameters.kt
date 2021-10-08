// WITH_RUNTIME

@JvmInline
value class Z(val x: Int)
@JvmInline
value class L(val x: Long)
@JvmInline
value class S(val x: String)

fun test(aZ: Z, aL: L, aS: S) = "${aZ.x} ${aL.x} ${aS.x}"

fun box(): String {
    if (::test.invoke(Z(1), L(1L), S("abc")) != "1 1 abc") throw AssertionError()

    return "OK"
}