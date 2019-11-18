// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FILE: Test.java

class Test {

    public static String test1() {
        return A.test1();
    }

    public static String test2() {
        return A.test2();
    }

    public static String test3() {
        return A.test3("JAVA");
    }

    public static String test4() {
        return A.getC();
    }

}

// FILE: simpleCompanionObject.kt

class A {

    companion object {
        val b: String = "OK"

        @JvmStatic val c: String = "OK"

        @JvmStatic fun test1() = b

        @JvmStatic fun test2() = b

        @JvmStatic fun String.test3() = this + b
    }
}

fun box(): String {
    if (Test.test1() != "OK") return "fail 1"

    if (Test.test2() != "OK") return "fail 2"

    if (Test.test3() != "JAVAOK") return "fail 3"

    if (Test.test4() != "OK") return "fail 4"

    return "OK"
}
