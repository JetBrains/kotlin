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

fun box(): String {
    return JavaCall().call {"OK"}
}
