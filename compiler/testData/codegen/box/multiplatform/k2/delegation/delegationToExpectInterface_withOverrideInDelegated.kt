// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect interface Base {
    fun foo(a: Int): String
    val s: String
}

class DelegatedImpl(val foo: Base) : Base by foo {
    override fun foo(a: Int): String {
        return "O"
    }
    override val s: String
        get() = "K"
}

// MODULE: platform()()(common)
// FILE: platform.kt
actual interface Base {
    actual fun foo(a: Int): String
    actual val s: String
}

class Impl : Base {
    override fun foo(a: Int): String = "Not"
    override val s: String = "OK"
}

fun box(): String {
    val x = DelegatedImpl(Impl())
    return x.foo(1) + x.s
}
