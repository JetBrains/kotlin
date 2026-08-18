// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: Test.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmExposeBoxed
fun f(p: UInt): Int = p.toInt()

// FILE: Main.java
public class Main {
    public String test() {
        try {
            TestKt.f(null);
        } catch (NullPointerException e) {
            return e.getMessage();
        }
        return "no exception";
    }
}

// FILE: Box.kt
fun box(): String {
    val message = Main().test()
    if (!message.startsWith("Parameter specified as non-null is null")) return "FAIL: $message"
    return "OK"
}
