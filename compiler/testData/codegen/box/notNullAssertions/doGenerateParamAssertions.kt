// TARGET_BACKEND: JVM
// DISABLE_CALL_ASSERTIONS
// MODULE: lib
// FILE: test/C.java
package test;

import org.jetbrains.annotations.NotNull;

public abstract class C<Type> {

    public abstract void doTest(@NotNull Type s);

    public static void runTest(C a) {
        try {
            a.doTest(null);
        } catch (NullPointerException e) {
            return;
        }
        throw new AssertionError("Fail: NullPointerException expected");
    }
}

// MODULE: main(lib)
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
