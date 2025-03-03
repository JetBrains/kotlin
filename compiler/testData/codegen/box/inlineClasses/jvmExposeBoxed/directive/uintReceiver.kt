// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// JVM_EXPOSE_BOXED

// FILE: Test.kt
class TopLevelClass {
    fun UInt.foo(): UInt = this
}

@OptIn(ExperimentalStdlibApi::class)
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
