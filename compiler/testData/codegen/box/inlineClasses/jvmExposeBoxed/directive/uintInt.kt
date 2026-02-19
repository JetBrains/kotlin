// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ImplicitJvmExposeBoxed

// FILE: Test.kt
class TopLevelClass {
    fun UInt.foo(i: Int): UInt = this + i.toUInt()
}

@OptIn(ExperimentalStdlibApi::class)
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
