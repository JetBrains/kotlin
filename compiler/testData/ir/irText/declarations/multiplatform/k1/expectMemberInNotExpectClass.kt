// IGNORE_BACKEND_K2: ANY
// ^^^ In FIR, declaring the same `expect` and `actual` classes in one compiler module is not possible (see KT-55177).

// SKIP_KLIB_TEST
// LANGUAGE: +MultiPlatformProjects

expect open class C1() {
    fun f(): String

    val p: Int
}

class C2 : C1()

actual open class C1 {
    actual fun f() = "O"

    actual val p = 42
}