// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// FILE: A.java
// ANDROID_ANNOTATIONS

import kotlin.annotations.jvm.internal.*;

class A {
    public int first(
        @ParameterName("first") @DefaultValue("42") int a,
        @ParameterName("second") @DefaultValue("1") int b
    ) {
        return 100 * a + b;
    }
}

// FILE: main.kt
fun box(): String {
    val a = A()
    if (a.first() != 100 * 42 + 1) {
        return "FAIL 1"
    }

    if (a.first(second = 2) != 100 * 42 + 2) {
        return "FAIL 2"
    }

    if (a.first(first = 2) != 100 * 2 + 1) {
        return "FAIL 3"
    }

    if (a.first(second = 2, first = 5) != 100 * 5 + 2) {
        return "FAIL 4"
    }

    return "OK"
}
