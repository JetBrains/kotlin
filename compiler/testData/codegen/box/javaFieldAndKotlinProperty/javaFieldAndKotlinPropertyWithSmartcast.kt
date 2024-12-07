// TARGET_BACKEND: JVM_IR
// COMMENTED[LANGUAGE: +ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty] uncomment when KT-56386 is fixed
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// Reason: KT-56386 is not fixed yet

// FILE: Jaba.java
public class Jaba {
    public String a = "O";
    public String b = "";
}

// FILE: test.kt
class My : Jaba() {
    private val a: String = "FAIL"
    private val b: String = "FAIL"
}

fun test(j: Any): String {
    if (j is My) {
        j.b = "K"
        return j.a + j.b
    }
    return "NO SMARTCAST"
}

fun box(): String = test(My())
