// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class StringWrapper @JvmExposeBoxed constructor(val s: String)

@JvmExposeBoxed
class Test(val s: StringWrapper) {
    fun ok(): String = s.s
}

// FILE: Main.java
public class Main {
    public String test() {
        return new Test(new StringWrapper("OK")).ok();
    }
}

// FILE: Box.kt
fun box(): String {
    return Main().test()
}