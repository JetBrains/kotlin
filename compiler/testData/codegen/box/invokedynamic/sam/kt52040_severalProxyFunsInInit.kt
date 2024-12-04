// TARGET_BACKEND: JVM
// FULL_JDK
// JVM_TARGET: 1.8
// FILE: J.java

import java.util.function.*;

public class J {
    public static String f1(Supplier<Object> r) {
        r.get();
        return "O";
    }

    public static String f2(Supplier<Object> r) {
        r.get();
        return "K";
    }
}

// FILE: test.kt

class C private constructor() {
    companion object {
        fun x1() = J.f1(::C)
        fun x2() = J.f2(::C)
    }
}

fun box(): String = C.x1() + C.x2()
