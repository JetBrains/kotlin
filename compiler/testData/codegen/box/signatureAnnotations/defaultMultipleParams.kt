// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// FILE: A.java
// ANDROID_ANNOTATIONS

import kotlin.annotations.jvm.internal.*;

class A {
    public int first(@DefaultValue("1") int a, @DefaultValue("2") int b) {
        return 100 * a + b;
    }

    public int second(int a, @DefaultValue("42") int b) {
        return 100 * a + b;
    }
}

// FILE: main.kt
fun box(): String {
    val a = A()

    if (a.first() != 102) {
        return "FAIL 1"
    }

    if (a.first(2) != 202) {
        return "FAIL 2"
    }

    if (a.first(3, 4) != 304)  {
        return "FAIL 3"
    }

    if (a.second(7, 8) != 708) {
        return "FAIL 4"
    }

    if (a.second(1) != 142) {
        return "FAIL 5"
    }

    return "OK"
}
