// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-68517

// MODULE: common
// FILE: common.kt
expect interface Base {
    fun foo(a: Int): String

    val s: String
}

class DelegatedImpl<T>(val foo: Base) : Base by foo {
    // (expect fun, DelegatedImpl)
}

// MODULE: platform()()(common)
// FILE: platform.kt
actual interface Base {
    actual fun foo(a: Int): String
    actual val s: String
}

class Impl : Base {
    override fun foo(a: Int): String = "O"

    override val s: String = "K"
}

fun box(): String {
    val x = DelegatedImpl<Int>(Impl())
    // (actual fun, DelegatedImpl)
    return x.foo(1) + x.s
}
