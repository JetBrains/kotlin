// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// tried to access field derived.Intermediate.a from class derived.Derived
// Field VS property: case "reference", protected field, invisible property

// FILE: BaseJava.java

package base;

public class BaseJava {
    protected String a = "OK";
}

// FILE: Derived.kt

package derived

import base.BaseJava

open class Intermediate : BaseJava() {
    private val a = "FAIL"
}

class Derived : Intermediate() {
    fun foo() = a
}

fun box(): String {
    val d = Derived()
    return d.foo()
}
