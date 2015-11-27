class OutPair<out X, out Y>(val x: X, val y: Y)
class In<in Z> {
    fun make(x: Z): String = x.toString()
}

@JvmSuppressWildcards(suppress = false)
interface A {
    fun foo(): OutPair<CharSequence, Number>
    fun bar(): In<String>
}

abstract class B : A {
    override fun foo(): OutPair<String, Int> = OutPair("OK", 123)
    override fun bar(): In<Any> = In()
}

fun box(): String {
    return JavaClass.test();
}
