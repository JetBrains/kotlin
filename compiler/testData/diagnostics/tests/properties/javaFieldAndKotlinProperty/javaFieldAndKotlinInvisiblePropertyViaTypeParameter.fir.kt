// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty
// ISSUE: KT-56386

// FILE: BaseJava.java
public class BaseJava {
    public String a = "OK";

    public String foo() {
        return a;
    }
}

// FILE: Derived.kt
open class Derived : BaseJava() {
    private val <!PROPERTY_HIDES_JAVA_FIELD!>a<!> = "FAIL"
}

fun <T : Derived> test(t: T): String {
    val first = t.<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!>
    if (first != "OK") return first
    if (t::<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!>.get() != "OK") return t::<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!>.get()
    t.<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!> = "12"
    if (t.foo() != "12") return "Error writing: ${t.foo()}"
    return "OK"
}

fun box(): String = test(Derived())
