// LANGUAGE: -ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty
// ISSUE: KT-56386

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
        super<My>.<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>b<!> = "K"
        return super<My>.<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!> + super<My>.<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>b<!>
    }
}

fun box(): String = Some().soo()
