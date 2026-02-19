// TARGET_BACKEND: JVM
// JVM_DEFAULT_MODE: no-compatibility
// JVM_TARGET: 1.8
// WITH_STDLIB

interface I {
    private fun foo() = 4

    fun bar() = { foo() + 5 }()
}
