// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

class C() : I1, C1()

expect open class C1() {
    fun o(): String

    val k: String
}

expect interface I1 {
    fun o(): String

    val k: String
}

fun f1(x: C1) = x.o()
fun f2(x: I1) = x.k

// MODULE: platform()()(common)
// FILE: platform.kt

actual open class C1 {
    actual fun o() = "O"

    actual val k = "K"
}

actual interface I1 {
    actual fun o(): String

    actual val k: String
}

fun box() = f1(C()) + f2(C())
