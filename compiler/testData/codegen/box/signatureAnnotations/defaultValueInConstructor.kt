// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// FILE: A.java
// ANDROID_ANNOTATIONS

import kotlin.annotations.jvm.internal.*;

public class A {
    public String x;
    public A(@DefaultValue("OK") String hello) {
        x = hello;
    }
}

// FILE: test.kt

fun box(): String {
    val a = A()

    val b = object : A() {
    }

    val c = object : A() {
        fun hello() = x
    }

    if (a.x != "OK") {
        return "FAIL 1"
    }

    if (b.x != "OK") {
        return "FAIL 2"
    }

    if (c.hello() != "OK") {
        return "FAIL 3"
    }

    return "OK"
}
