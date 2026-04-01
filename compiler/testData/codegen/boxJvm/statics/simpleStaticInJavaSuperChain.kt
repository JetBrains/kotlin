// TARGET_BACKEND: JVM

// FILE: A.java

public class A {
    public static String s = "A.s: NOT OK";
    public static String f() {
        return "A.f: NOT OK";
    }

    public static class B extends A {
        public static String s = "OK";
        public static String f() {
            return "OK";
        }
    }
}


// FILE: Kotlin.kt

class Kotlin: A.B() {
    fun getS() = s
    fun callF() = f()
}

fun box(): String {
    val kotlin = Kotlin()
    if (kotlin.getS() != "OK") return "fail1"
    return kotlin.callF()
}
