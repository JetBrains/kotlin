// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(kotlin.ExperimentalStdlibApi::class)

@JvmInline
value class StringWrapper(val s: String) // This getter is exposed by default

@JvmExposeBoxed
@JvmInline
value class StringWrapper2(val s1: StringWrapper) // This is not

@JvmExposeBoxed("create")
fun createWrapper(): StringWrapper2 = StringWrapper2(StringWrapper("OK"))

// FILE: Main.java
public class Main {
    public String test() {
        return ICKt.create().getS1().getS();
    }
}

// FILE: Box.kt
fun box(): String {
    return Main().test()
}