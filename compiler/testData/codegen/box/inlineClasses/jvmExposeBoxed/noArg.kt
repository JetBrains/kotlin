// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@OptIn(ExperimentalStdlibApi::class)
@JvmInline
@JvmExposeBoxed
value class StringWrapper(val s: String = "OK")

// FILE: Main.java
public class Main {
    public String test() {
        return new StringWrapper().getS();
    }
}

// FILE: Box.kt
fun box(): String {
    return Main().test()
}