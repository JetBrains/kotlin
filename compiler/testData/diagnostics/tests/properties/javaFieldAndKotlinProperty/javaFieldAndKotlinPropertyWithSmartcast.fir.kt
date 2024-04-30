// LANGUAGE: -ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty
// ISSUE: KT-56386

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
        j.<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>b<!> = "K"
        return j.<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!> + j.<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>b<!>
    }
    return "NO SMARTCAST"
}

fun box(): String = test(My())
