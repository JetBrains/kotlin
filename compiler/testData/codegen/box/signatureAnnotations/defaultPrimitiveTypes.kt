// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// FILE: A.java
// ANDROID_ANNOTATIONS

import kotlin.annotations.jvm.internal.*;

public class A {

    public int a(@DefaultValue("42") int arg) {
        return arg;
    }

    public float b(@DefaultValue("42.5") float arg) {
        return arg;
    }

    public boolean c(@DefaultValue("true") boolean arg) {
        return arg;
    }

    public byte d(@DefaultValue("42") byte arg) {
        return arg;
    }

    public char e(@DefaultValue("o") char arg) {
        return arg;
    }

    public double f(@DefaultValue("1e12") double arg) {
        return arg;
    }

    public String g(@DefaultValue("hello") String arg) {
        return arg;
    }

    public long h(@DefaultValue("42424242424242") long arg) {
        return arg;
    }

    public short i(@DefaultValue("123") short arg) {
        return arg;
    }
}

// FILE: test.kt
fun box(): String {
    val a = A()

    if (a.a() != 42) {
        return "FAIL Int: ${a.a()}"
    }

    if (a.b() != 42.5f) {
        return "FAIL Float: ${a.b()}"
    }

    if (!a.c()) {
        return "FAIL Boolean: ${a.c()}"
    }

    if (a.d() != 42.toByte()) {
        return "FAIL Byte: ${a.d()}"
    }

    if (a.e() != 'o') {
        return "FAIL Char: ${a.e()}"
    }

    if (a.f() != 1e12) {
        return "FAIl Double: ${a.f()}"
    }

    if (a.g() != "hello") {
        return "FAIL String: ${a.g()}"
    }

    if (a.h() != 42424242424242) {
        return "FAIL Long: ${a.h()}"
    }

    if (a.i() != 123.toShort()) {
        return "FAIL Short: ${a.i()}"
    }

    return "OK"
}
