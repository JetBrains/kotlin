// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: Test.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmExposeBoxed
class TopLevelClass {
    fun UInt.foo(i: Int): UInt = this + i.toUInt()
}

@JvmExposeBoxed("create")
fun createUInt(): UInt = 1u

// FILE: Main.java
public class Main {
    public kotlin.UInt test() {
        return new TopLevelClass().foo(TestKt.create(), 2);
    }
}

// FILE: Box.kt
fun box(): String {
    if (Main().test() == 3u) return "OK"
    return "FAIL"
}
