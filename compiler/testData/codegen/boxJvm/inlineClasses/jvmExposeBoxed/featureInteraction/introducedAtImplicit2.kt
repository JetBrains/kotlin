// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// JVM_EXPOSE_BOXED

// FILE: Test.kt
@file:OptIn(ExperimentalStdlibApi::class, ExperimentalVersionOverloading::class)

class MyClass(val s: String = "OK_implicit", @IntroducedAt(version = "3") val property: UInt = 2u)

@JvmExposeBoxed("create")
fun createUInt(): UInt = 1u

// FILE: Main.java
public class Main {
    public MyClass test1() {
        return new MyClass("OK", TestKt.create());
    }

    // @IntroducedAt hides a constructor with single parameter

    public MyClass test3() {
        return new MyClass();
    }
}

// FILE: box.kt

fun box(): String {
    val main = Main()
    if (main.test1().property != 1u || main.test1().s != "OK") return "FAIL 1: ${main.test1().property} ${main.test1().s}"
    if (main.test3().property != 2u || main.test3().s != "OK_implicit") return "FAIL 3: ${main.test3().property} ${main.test3().s}"
    return "OK"
}
