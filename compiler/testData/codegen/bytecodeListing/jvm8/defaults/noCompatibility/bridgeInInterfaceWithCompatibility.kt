// WITH_STDLIB
// JVM_DEFAULT_MODE: no-compatibility
// IGNORE_BACKEND_K1: JVM_IR

// This test is the same as `bridgeInInterface.kt`, but with `@JvmDefaultWithCompatibility`.
// So it's important that TestClass has both the specialized method and the bridge generated.

interface Test<T> {
    fun test(p: T): T {
        return null!!
    }
}

interface Test2: Test<String> {
    override fun test(p: String): String {
        return p
    }
}

@JvmDefaultWithCompatibility
class TestClass : Test2
