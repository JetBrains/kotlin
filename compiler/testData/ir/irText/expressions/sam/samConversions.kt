// TARGET_BACKEND: JVM
// WITH_JDK

// FILE: samConversions.kt
fun J.test0(a: Runnable) {
    J.runStatic(a)
    runIt(a)
}

fun test1() {
    J.runStatic { test1() }
}

fun J.test2() {
    runIt { test1() }
}

fun J.test3(a: () -> Unit) {
    run2(a, a)
}

fun J.test4(a: () -> Unit, b: () -> Unit, flag: Boolean) {
    runIt(if (flag) a else b)
}

// FILE: J.java
public class J {
    public static void runStatic(Runnable r) {}

    public void runIt(Runnable r) {}

    public void run2(Runnable r1, Runnable r2) {}
}
