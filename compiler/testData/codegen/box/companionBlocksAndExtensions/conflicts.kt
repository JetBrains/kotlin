// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND_K1: ANY
// WITH_SDLIB

// Note that the JVM forbids defining a static and instance method with the same signature.
class A {
    val a: String = "memberVal"
    fun foo(s: String) = "memberFun: $s"
    companion object {
        val a: String = "companionObjectVal"
        fun foo(s: String) = "companionObjectFun: $s"
        @JvmStatic
        val a: String = "companionObjectStaticVal"
        @JvmStatic
        fun foo(s: String) = "companionObjectStaticFun: $s"
    }
    companion {
        val a: String = "companionBlockVal"
        fun foo(s: String) = "companionBlockFun: $s"
    }

}

fun box(): String { //todo: assertEquals instead of if &&
    return A().a + A().foo("a") + A.a + A.foo("b") + A.Companion.a + A.Companion.foo("b"))
}
