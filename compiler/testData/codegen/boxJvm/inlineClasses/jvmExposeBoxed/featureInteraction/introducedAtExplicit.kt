// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: Test.kt
@file:OptIn(ExperimentalStdlibApi::class, ExperimentalVersionOverloading::class)

class MyClass @JvmExposeBoxed constructor(@IntroducedAt(version = "3") val property: UInt = 2u)

@JvmExposeBoxed("create")
fun createUInt(): UInt = 1u

// FILE: Main.java
public class Main {
    public MyClass test1() {
        return new MyClass(TestKt.create());
    }
}

// FILE: box.kt

fun box(): String {
    val main = Main()
    if (main.test1().property != 1u) return "FAIL 1: ${main.test1().property}"
    return "OK"
}
