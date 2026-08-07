// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Inner(val s: String)

@JvmInline
@JvmExposeBoxed
value class Outer(val inner: Inner)

@JvmExposeBoxed
fun unwrap(outer: Outer): String = outer.inner.s

// FILE: Main.java
public class Main {
    public String test() {
        return ICKt.unwrap(new Outer(new Inner("OK")));
    }
}

// FILE: Box.kt
fun box(): String = Main().test()
