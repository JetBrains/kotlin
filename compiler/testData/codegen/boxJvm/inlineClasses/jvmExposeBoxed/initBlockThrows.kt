// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Positive(val value: Int) {
    init {
        require(value >= 0) { "negative: $value" }
    }
}

// FILE: Main.java
public class Main {
    public String test() {
        try {
            new Positive(-1);
            return "no exception";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}

// FILE: Box.kt
fun box(): String {
    // The boxed constructor runs the init block, so the validation has to reach the Java caller.
    val res = Main().test()
    if (res != "negative: -1") return "FAIL: $res"
    return "OK"
}
