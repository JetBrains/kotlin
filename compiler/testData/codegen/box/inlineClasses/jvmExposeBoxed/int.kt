// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: Test.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmExposeBoxed("create")
fun createUInt(): UInt = 1u

@JvmExposeBoxed
fun foo(u: UInt): Int = u.toInt()

// FILE: Main.java
public class Main {
    public int test() {
        return TestKt.foo(TestKt.create());
    }
}

// FILE: Box.kt
fun box(): String {
    if (Main().test() == 1) return "OK"
    return "FAIL"
}
