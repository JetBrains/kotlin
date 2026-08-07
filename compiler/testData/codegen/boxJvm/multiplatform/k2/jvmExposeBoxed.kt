// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect value class Id(val value: String)

expect fun makeId(value: String): Id

// MODULE: main()()(common)
// FILE: test.kt
@file:OptIn(ExperimentalStdlibApi::class)

// '@JvmExposeBoxed' is JVM-only, so it can only be written on the actual declaration. It has to survive
// actualization and still produce the boxed variant next to the mangled one.
@JvmInline
@JvmExposeBoxed
actual value class Id(actual val value: String)

@JvmExposeBoxed("createId")
actual fun makeId(value: String): Id = Id(value)

// FILE: Main.java
public class Main {
    public String test() {
        return TestKt.createId("OK").getValue();
    }
}

// FILE: box.kt
fun box(): String = Main().test()
