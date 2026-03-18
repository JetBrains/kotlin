// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ImplicitJvmExposeBoxed

// FILE: IC.kt
@JvmInline
value class StringWrapper(val s: String) {
    init {
        result = s
    }
}

var result = "FAIL"

// FILE: Main.java
public class Main {
    public void test() {
        new StringWrapper("OK");
    }
}

// FILE: Box.kt
fun box(): String {
    Main().test()
    return result
}