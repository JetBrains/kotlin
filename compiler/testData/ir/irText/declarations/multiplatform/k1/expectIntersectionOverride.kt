// IGNORE_BACKEND_K2: ANY
// ^^^ In FIR, declaring the same `expect` and `actual` classes in one compiler module is not possible (see KT-55177).

// !LANGUAGE: +MultiPlatformProjects

interface I1 {
    fun f(): String
    val p: Int
}

interface I2 {
    fun f(): String
    val p: Int
}

expect class C() : I1, I2 {
    override fun f(): String
    override val p: Int
}

actual class C : I1, I2 {
    actual override fun f() = "OK"
    actual override val p = 42
}