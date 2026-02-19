// TARGET_BACKEND: JVM_IR
// COMMENTED[LANGUAGE: +ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty] uncomment when KT-56386 is fixed
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// Reason: KT-56386 is not fixed yet

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
