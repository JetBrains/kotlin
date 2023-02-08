// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

// FILE: Jaba.java

public class Jaba {
    public String a = "OK";
}

// FILE: test.kt
class My : Jaba() {
    private val a: String = "FAIL"

    operator fun plus(my: My) = my
}

fun create(): My? = My()

fun box(): String {
    return (create() ?: My()).a
}
