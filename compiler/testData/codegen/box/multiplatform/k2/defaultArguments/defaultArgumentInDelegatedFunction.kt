// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-62671

// MODULE: common
// FILE: common.kt
interface A {
    fun foo(x: Int = 1): String
}

class B : A  {
    override fun foo(x: Int): String {
        return if (x == 1) "OK" else "Fail: $x"
    }
}

class X(val delegate: A = B()) : A by delegate

// MODULE: platform()()(common)
// FILE: platform.kt
fun box(): String {
    val x = X()
    return x.foo()
}
