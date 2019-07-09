// KOTLIN_CONFIGURATION_FLAGS: +JVM.DISABLE_CALL_ASSERTIONS
// FILE: C.java
package test;

import org.jetbrains.annotations.NotNull;

public abstract class C<Type> {

    public abstract void doTest(@NotNull Type s);

    public static void runTest(C a) {
        try {
            a.doTest(null);
        } catch (IllegalArgumentException e) {
            return;
        }
        throw new AssertionError("Fail: IllegalArgumentException expected");
    }
}

// FILE: B.kt
import test.C

class TestString : C<String>() {
    override fun doTest(s: String) { }
}

class TestUnit : C<Unit>() {
    override fun doTest(s: Unit) { }
}

fun box(): String {
    C.runTest(TestString())
    C.runTest(TestUnit())
    return "OK"
}
