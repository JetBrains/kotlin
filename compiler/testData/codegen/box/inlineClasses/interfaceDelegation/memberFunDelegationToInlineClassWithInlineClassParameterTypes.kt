// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// JVM_ABI_K1_K2_DIFF: KT-63855

import kotlin.test.assertEquals

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val x: String)

interface IFoo<T> {
    fun memberFun(s1: S, s2: String): String
    fun memberFunT(x1: T, x2: String): String
    fun <X> genericMemberFun(x1: T, x2: X): String
    fun S.memberExtFun(s: String): String
    fun T.memberExtFunT(x: String): String
    fun <X> T.genericMemberExtFun(x: X): String
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class FooImpl(val xs: Array<String>) : IFoo<S> {
    override fun memberFun(s1: S, s2: String): String = xs[0] + s1.x + s2
    override fun memberFunT(x1: S, x2: String): String = xs[0] + x1.x + x2
    override fun <X> genericMemberFun(x1: S, x2: X): String = xs[0] + x1.x + x2.toString()
    override fun S.memberExtFun(s: String): String = xs[0] + this.x + s
    override fun S.memberExtFunT(x: String): String = xs[0] + this.x + x
    override fun <X> S.genericMemberExtFun(x: X): String = xs[0] + this.x + x.toString()
}

class Test : IFoo<S> by FooImpl(arrayOf("1"))

fun box(): String {
    val test = Test()

    assertEquals("1OK", test.memberFun(S("O"), "K"))
    assertEquals("1OK", test.memberFunT(S("O"), "K"))
    assertEquals("1OK", test.genericMemberFun(S("O"), "K"))

    with(test) {
        assertEquals("1OK", S("O").memberExtFun("K"))
        assertEquals("1OK", S("O").memberExtFunT("K"))
        assertEquals("1OK", S("O").genericMemberExtFun("K"))
    }

    return "OK"
}
