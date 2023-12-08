// IGNORE_BACKEND_K1: ANY
// ^^^ K1 as well as K1-based test infra do not support "fragment refinement".

// FIR_IDENTICAL
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect abstract class A protected constructor() {
    abstract fun foo()
}

expect open class B(i: Int): A {
    override fun foo()
    open fun bar(s: String)
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual abstract class A protected actual constructor() {
    actual abstract fun foo()
}

actual open class B actual constructor(i: Int): A() {
    actual override fun foo() {}
    actual open fun bar(s: String) {}
}
