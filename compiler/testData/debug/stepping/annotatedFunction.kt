// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// FILE: test.kt
inline fun g() = ""

@JvmName("jvmName")
inline fun f(
    p0: Int,
    p1: String = "O",
    p2: String = "",
    p3: () -> String = { "K" },
    p4: String = g(),
): String = p1 + p2 + p3() + p4

fun box(): String = f(0)

// EXPECTATIONS JVM_IR
// test.kt:16 box
// test.kt:10 box
// test.kt:11 box
// test.kt:13 box
// test.kt:5 box
// test.kt:13 box
// test.kt:14 box
// test.kt:12 box
// test.kt:14 box
// test.kt:16 box
