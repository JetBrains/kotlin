// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63984

// FILE: Test.java

class Test {
    public static String foo() {
        return Annotation.getTEST_FIELD();
    }

    public static String foo2() {
        return Annotation.getTEST_FIELD2();
    }

    public static void foo2Set() {
        Annotation.setTEST_FIELD2("OK");
    }
}

// FILE: kt31389.kt

annotation class Annotation {
    companion object {
        @JvmStatic val TEST_FIELD = "OK"

        var TEST_FIELD2 = ""
            @JvmStatic get
            @JvmStatic set
    }
}

fun box(): String {
    if (Test.foo() != "OK") return "Fail 1: ${Test.foo()}"
    Test.foo2Set()
    return Test.foo2()
}
