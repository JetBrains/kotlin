// IGNORE_BACKEND_K2: ANY
// ^^^ In FIR, declaring the same `expect` and `actual` classes in one compiler module is not possible (see KT-55177).

// !LANGUAGE: +MultiPlatformProjects

expect abstract class A protected constructor() {
    abstract fun foo()
}

expect open class B(i: Int): A {
    override fun foo()
    open fun bar(s: String)
}

actual abstract class A protected actual constructor() {
    actual abstract fun foo()
}

actual open class B actual constructor(i: Int): A() {
    actual override fun foo() {}
    actual open fun bar(s: String) {}
}
