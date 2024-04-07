// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8

// FILE: Test.java
public interface Test<T> {
    default T test(T p) {
        return null;
    }
}

// FILE: kotlin.kt
interface Test2: Test<String> {
    override fun test(p: String): String {
        return p
    }

    fun forDefaultImpls() {}
}

class TestClass : Test2

fun box() = TestClass().test("OK")
