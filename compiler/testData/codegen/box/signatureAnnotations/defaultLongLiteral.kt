// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// FILE: A.java
// ANDROID_ANNOTATIONS

import kotlin.annotations.jvm.internal.*;

public class A {
    public Long first(@DefaultValue("0x1F") Long value) {
        return value;
    }

    public Long second(@DefaultValue("0X1F") Long value) {
        return value;
    }

    public Long third(@DefaultValue("0b1010") Long value) {
        return value;
    }

    public Long fourth(@DefaultValue("0B1010") Long value) {
        return value;
    }
}

// FILE: test.kt
fun box(): String {
    val a = A()

    if (a.first() != 0x1F.toLong()) {
        return "FAIL 1"
    }

    if (a.second() != 0x1F.toLong()) {
        return "FAIL 2"
    }

    if (a.third() != 0b1010.toLong()) {
        return "FAIL 3"
    }

    if (a.fourth() != 0b1010.toLong()) {
        return "FAIL 4"
    }

    return "OK"
}

