// LANGUAGE: -ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty
// ISSUE: KT-56386

// FILE: base/BaseJava.java
package base;

public class BaseJava {
    protected String a = "";
}

// FILE: Derived.kt
package derived

import base.BaseJava

open class Intermediate : BaseJava() {
    private val a = "FAIL"
}

class Derived : Intermediate() {
    fun foo() = <!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!>

    fun bar() {
        <!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!> = "OK"
    }
}

fun box(): String {
    val d = Derived()
    d.bar()
    return d.foo()
}
