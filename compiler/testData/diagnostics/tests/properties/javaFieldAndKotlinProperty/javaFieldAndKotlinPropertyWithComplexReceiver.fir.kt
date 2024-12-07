// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty
// ISSUE: KT-56386

// FILE: Jaba.java
public class Jaba {
    public String a = "OK";
}

// FILE: test.kt
class My : Jaba() {
    private val <!PROPERTY_HIDES_JAVA_FIELD!>a<!>: String = "FAIL"

    operator fun plus(my: My) = my
}

fun create(): My? = My()

fun box(): String {
    return (create() ?: My()).<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!>
}
