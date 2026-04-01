// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: Test.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmExposeBoxed
class TopLevelClass {
    fun UInt.foo(): UInt = this
}

@JvmExposeBoxed("create")
fun createUInt(): UInt = 1u

// FILE: Main.java
public class Main {
    public kotlin.UInt test() {
        return new TopLevelClass().foo(TestKt.create());
    }
}

// FILE: Box.kt
fun box(): String {
    if (Main().test() == 1u) return "OK"
    return "FAIL"
}
