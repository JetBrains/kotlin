// FULL_JDK
// JVM_TARGET: 1.8
// FILE: J.java

import java.util.function.*;

public class J {
    public static void f1(Supplier<Object> r) {}
    public static void f2(Supplier<Object> r) {}
}

// FILE: test.kt

class C private constructor() {
    companion object {
        fun x1() = J.f1(::C)
        fun x2() = J.f2(::C)
    }
}
