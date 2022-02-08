// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// FILE: Simple.java

public interface Simple {
    default String test() {
        return "O";
    }

    static String testStatic() {
        return "K";
    }
}

// FILE: main.kt
interface KSimple : Simple {}

class TestClass : KSimple {
    override fun test(): String {
        return super.test()
    }
}


fun box(): String {
    return TestClass().test() + Simple.testStatic()
}
