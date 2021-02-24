// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_RUNTIME
// FILE: JavaRunner.java
public class JavaRunner {
    public static void runTwice(Runnable runnable) {
        runnable.run();
        runnable.run();
    }
}

// FILE: nonTrivialReceiver.kt
class A() {
    fun f() {}
}

fun box(): String {
    var x = 0
    JavaRunner.runTwice({ x++; A() }()::f)
    if (x != 1) return "Fail"
    return "OK"
}
