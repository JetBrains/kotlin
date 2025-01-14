// JVM_DEFAULT_MODE: all-compatibility

// This test is checking that:
// 1) Test2 has both the specialized method (with String in the signature) and the bridge (with Any)
// 2) Test2 has a DefaultImpls class with a static version of the specialized method, but not the bridge
// 3) Before KT-73954 is resolved, TestClass has neither the specialized method, nor the bridge

// FILE: Test.java

public interface Test<T> {
    default T test(T p) {
        return null;
    }
}

// FILE: kotlin.kt

interface Test2 : Test<String> {
    override fun test(p: String): String {
        return p
    }

    fun forDefaultImpls() {}
}

class TestClass : Test2
