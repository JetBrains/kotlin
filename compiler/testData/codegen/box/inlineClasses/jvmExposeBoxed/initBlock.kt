// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: IC.kt
@OptIn(ExperimentalStdlibApi::class)
@JvmInline
@JvmExposeBoxed
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