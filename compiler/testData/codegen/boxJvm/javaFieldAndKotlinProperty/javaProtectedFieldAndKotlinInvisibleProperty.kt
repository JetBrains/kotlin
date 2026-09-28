// TARGET_BACKEND: JVM
// COMMENTED[LANGUAGE: +ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty] uncomment when KT-56386 is fixed
// IGNORE_BACKEND: JVM
// IGNORE_HEADER_MODE: JVM
// Reason: KT-56386 is not fixed yet

// FILE: BaseJava.java
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
    fun foo() = a

    fun bar() {
        a = "OK"
    }
}

fun box(): String {
    val d = Derived()
    d.bar()
    return d.foo()
}
