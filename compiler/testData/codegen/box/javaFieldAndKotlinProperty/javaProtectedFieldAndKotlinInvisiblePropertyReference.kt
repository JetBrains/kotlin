// TARGET_BACKEND: JVM_IR
// COMMENTED[LANGUAGE: +ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty] uncomment when KT-56386 is fixed
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
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

// TODO: remove suppress after dropping the relevant diagnostic
@Suppress("JAVA_SHADOWED_PROTECTED_FIELD_REFERENCE")
class Derived : Intermediate() {
    fun foo() = this::a.get()

    fun bar() {
        Derived::a.set(this, "OK")
    }
}

fun box(): String {
    val d = Derived()
    d.bar()
    return d.foo()
}
