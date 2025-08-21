// JVM_DEFAULT_MODE: enable

// This test is checking that:
// 1) Test2 has both the specialized method (with String in the signature) and the bridge (with Any)
// 2) Test2 has a DefaultImpls class with a static version of the specialized method, but not the bridge
// 3) TestClass has both the specialized method and the bridge (see KT-73954)

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
