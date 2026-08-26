// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-88769

// MODULE: lib-common
interface A {
    fun foo(x: Int, y: String = "OK"): String
}

interface B : A

object AImpl : A {
    override fun foo(x: Int, y: String): String = y
}

// MODULE: lib-platform()()(lib-common)


// MODULE: app-common(lib-common)
expect class C : B {
    override fun foo(x: Int, y: String): String
}

// MODULE: app-platform(lib-platform)()(app-common)
actual class C : B, A by AImpl {}

fun box(): String {
    return C().foo(1)
}
