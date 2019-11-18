// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: J.java

public class J {
    public static int f() {
       return A.Companion.getI1() + A.Companion.getI2() + B.Named.getI1() + B.Named.getI2();
    }
}

// FILE: test.kt

class A {
    companion object {
        val i1 = 1
        val i2 = 2
    }
}

class B {
    companion object Named {
        val i1 = 3
        val i2 = 4
    }
}

fun box(): String {
    return if (J.f() == A.i1 + A.i2 + B.i1 + B.i2) "OK" else "Fail: ${J.f()}"
}
