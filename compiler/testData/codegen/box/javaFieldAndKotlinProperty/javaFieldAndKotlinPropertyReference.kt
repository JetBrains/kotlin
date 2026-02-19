// TARGET_BACKEND: JVM_IR
// COMMENTED[LANGUAGE: +ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty] uncomment when KT-56386 is fixed
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// Reason: KT-56386 is not fixed yet

// FILE: BaseJava.java
public class BaseJava {
    public String a = "OK";
}

// FILE: Derived.kt
class Derived : BaseJava() {
    private val a = "FAIL"
}

fun box(): String {
    val d = Derived()
    return d::a.get()
}
