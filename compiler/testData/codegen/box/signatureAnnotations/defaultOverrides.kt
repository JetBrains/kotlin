// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// FILE: A.java
// ANDROID_ANNOTATIONS

import kotlin.annotations.jvm.internal.*;

class A {
    public int first(@DefaultValue("42") int a) {
        return a;
    }
}

// FILE: B.java
class B extends A {
    public int first(int a) {
        return a;
    }
}

// FILE: test.kt
fun box(): String {
    val a = A()
    val b = B()
    val ab: A = B()

    if (a.first() != 42) {
        return "FAIL 1"
    }
    if (b.first() != 42) {
        return "FAIL 2"
    }
    if (ab.first() != 42) {
        return "FAIL 4"
    }

    return "OK"
}