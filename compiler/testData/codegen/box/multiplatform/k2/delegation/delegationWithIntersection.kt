// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect interface Base1 {
    fun foo(a: String): String
}

expect interface Base2 {
    fun foo(a: Any): Any
}

class DelegatedImpl(val a: Base1, val b: Base2) : Base1 by a, Base2 by b

// MODULE: platform()()(common)
// FILE: main.kt
actual interface Base1 {
    actual fun foo(a: String): String
}

actual interface Base2 {
    actual fun foo(a: Any): Any
}

class Base1Impl: Base1 {
    override fun foo(a: String): String {
        return "O"
    }
}

class Base2Impl: Base2 {
    override fun foo(a: Any): Any {
        return "K"
    }
}

fun box(): String {
    val x = DelegatedImpl(Base1Impl(), Base2Impl())
    return x.foo("") + x.foo(1)
}