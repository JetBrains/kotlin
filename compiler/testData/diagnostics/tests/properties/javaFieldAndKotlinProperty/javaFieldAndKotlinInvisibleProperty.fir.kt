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
class Derived : BaseJava() {
    private val a = "FAIL"
}

fun box(): String {
    val first = Derived().<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!>
    if (first != "OK") return first
    val d = Derived()
    if (d::<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!>.get() != "OK") return d::<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!>.get()
    d.<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!> = "12"
    if (d.foo() != "12") return "Error writing: ${d.foo()}"
    return "OK"
}
