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
open class My : Jaba() {
    private val a: String = "FAIL"
    private val b: String = "FAIL"
}

class Some : My() {
    fun soo(): String {
        super<My>.b = "K"
        return super<My>.a + super<My>.b
    }
}

fun box(): String = Some().soo()
