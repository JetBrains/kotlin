// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ContextParameters

// FILE: J.java
public class J {
    public static String box() {
        return new A().f(new Z("O"), new Z("K"));
    }
}

// FILE: box.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Z(val value: String)

@JvmExposeBoxed
class A {
    fun Z.f(k: Z): String = this.value + k.value
}

fun box(): String = J.box()
