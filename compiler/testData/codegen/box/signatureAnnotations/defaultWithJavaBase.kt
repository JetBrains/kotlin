// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// FILE: A.java
// ANDROID_ANNOTATIONS

import kotlin.annotations.jvm.internal.*;

public class A {
    public int x(@DefaultValue("42") int x) {
        return x;
    }
}

// FILE: B.kt
class B : A() {
    override fun x(x: Int): Int = x + 1
}

// FILE: box.kt
fun box(): String {
    if (B().x() != 43) {
        return "FAIL"
    }

    return "OK"
}
