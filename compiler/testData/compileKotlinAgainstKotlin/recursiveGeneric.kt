// FILE: A.kt

package a

interface Rec<R, out T: Rec<R, T>> {
    fun t(): T
}

interface Super {
    fun foo(p: Rec<*, *>) = p.t()
}

// FILE: B.kt

import a.*

fun main(args: Array<String>) {
    val declaredMethod = Super::class.java.getDeclaredMethod("foo", Rec::class.java)
    val genericString = declaredMethod.toGenericString()
    if (genericString != "public abstract a.Rec<?, ?> a.Super.foo(a.Rec<?, ?>)") throw AssertionError(genericString)
}

fun test(s: Super, p: Rec<*, *>) {
    s.foo(p).t().t().t()
}
