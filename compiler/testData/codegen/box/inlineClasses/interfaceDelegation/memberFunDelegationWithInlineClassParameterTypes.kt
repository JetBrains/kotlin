// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.assertEquals

inline class S(val x: String)

interface IFoo<T> {
    fun memberFun(s1: S, s2: String): String
    fun memberFunT(x1: T, x2: String): String
    fun <X> genericMemberFun(x1: T, x2: X): String
    fun S.memberExtFun(s: String): String
    fun T.memberExtFunT(x: String): String
    fun <X> T.genericMemberExtFun(x: X): String
}

object FooImpl : IFoo<S> {
    override fun memberFun(s1: S, s2: String): String = s1.x + s2
    override fun memberFunT(x1: S, x2: String): String = x1.x + x2
    override fun <X> genericMemberFun(x1: S, x2: X): String = x1.x + x2.toString()
    override fun S.memberExtFun(s: String): String = this.x + s
    override fun S.memberExtFunT(x: String): String = this.x + x
    override fun <X> S.genericMemberExtFun(x: X): String = this.x + x.toString()
}

class Test : IFoo<S> by FooImpl

fun box(): String {
    val test = Test()

    assertEquals("OK", test.memberFun(S("O"), "K"))
    assertEquals("OK", test.memberFunT(S("O"), "K"))
    assertEquals("OK", test.genericMemberFun(S("O"), "K"))

    with(test) {
        assertEquals("OK", S("O").memberExtFun("K"))
        assertEquals("OK", S("O").memberExtFunT("K"))
        assertEquals("OK", S("O").genericMemberExtFun("K"))
    }

    return "OK"
}
