// TARGET_BACKEND: JVM
// FILE: JavaCall.java

class JavaCall {
    String call(Test test) {
        return test.call();
    }
}

// FILE: Test.java

interface Test {

    String call();

    default String test() {
        return "K";
    }

    static String testStatic() {
        return "K";
    }
}

// FILE: sam.kt
// JVM_TARGET: 1.8

fun box(): String {
    val lambda = { "X" }
    if (JavaCall().call(lambda) != "X") return "Fail"

    return JavaCall().call {"OK"}
}
