// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect interface Base {
    fun foo(a: String): String
}

class DelegatedImpl(val foo: Base) : Base by foo

// MODULE: platform()()(common)
// FILE: platform.kt
actual interface Base {
    actual fun foo(a: String): String
    fun foo(a: Any): Any
}

class Impl : Base {
    override fun foo(a: String): String = "O"
    override fun foo(a: Any): Any = "K"
}

fun box(): String {
    val x = DelegatedImpl(Impl())
    return x.foo("") + x.foo(1)
}