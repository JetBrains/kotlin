// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty
// ISSUE: KT-56386

// FILE: BaseJava.java
public class BaseJava {
    public String a = "OK";
}

// FILE: Derived.kt
class Derived : BaseJava() {
    private val <!PROPERTY_HIDES_JAVA_FIELD!>a<!> = "FAIL"
}

fun box(): String {
    val d = Derived()
    return d::<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!>.get()
}
