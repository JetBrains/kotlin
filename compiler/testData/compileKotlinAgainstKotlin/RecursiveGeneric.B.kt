import a.*

fun main(args: Array<String>) {
    val declaredMethod = javaClass<Super>().getDeclaredMethod("foo", javaClass<Rec<*, *>>())
    val genericString = declaredMethod.toGenericString()
    if (genericString != "public abstract a.Rec<?, ?> a.Super.foo(a.Rec<?, ?>)") throw AssertionError(genericString)
}

fun test(s: Super, p: Rec<*, *>) {
    s.foo(p).t().t().t()
}