class OutPair<out X, out Y>(val x: X, val y: Y)
class In<in Z> {
    fun make(x: Z): String = x.toString()
}

interface A {
    fun foo(): OutPair<@JvmWildcard CharSequence, @JvmSuppressWildcards(false) Number>
    fun bar(): In<@JvmWildcard String>
}

abstract class B : A {
    override fun foo(): OutPair<String, Int> = OutPair("OK", 123)
    override fun bar(): In<Any> = In()
}

fun box(): String {
    return JavaClass.test();
}
