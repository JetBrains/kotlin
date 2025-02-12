// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// MODULE: lib
// FILE: A.kt

package a

interface Rec<R, out T: Rec<R, T>> {
    fun t(): T
}

interface Super {
    fun foo(p: Rec<*, *>) = p.t()
}

// MODULE: main(lib)
// FILE: B.kt

import a.*

fun box(): String {
    val declaredMethod = Super::class.java.getDeclaredMethod("foo", Rec::class.java)
    val genericString = declaredMethod.toGenericString()
        .substringAfter("public default ") // In K2, `Super.foo` is default because of LanguageFeature.JvmDefaultEnableByDefault.
        .substringAfter("public abstract ") // In K1, `Super.foo` is abstract.
    if (genericString != "a.Rec<?, ?> a.Super.foo(a.Rec<?, ?>)") return "Fail: $genericString"
    return "OK"
}

fun test(s: Super, p: Rec<*, *>) {
    s.foo(p).t().t().t()
}
