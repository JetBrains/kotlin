// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58003

// MODULE: common
// FILE: common.kt

class C3 : C2()

open class C2 : C1()

expect open class C1() {
    fun o(): String

    val k: String
}

fun foo(c3: C3) = c3.o() + c3.k

// MODULE: platform()()(common)
// FILE: platform.kt

actual open class C1 {
    actual fun o() = "O"

    actual val k = "K"
}

fun box() = foo(C3())
