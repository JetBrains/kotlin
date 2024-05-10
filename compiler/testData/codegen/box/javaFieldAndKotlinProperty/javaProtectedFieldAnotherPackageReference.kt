// TARGET_BACKEND: JVM_IR
// IGNORE_CODEGEN_WITH_FIR2IR_FAKE_OVERRIDE_GENERATION
// FILE: base/BaseJava.java

package base;

public class BaseJava {
    protected String a = "OK";
}

// FILE: Derived.kt

package derived

import base.BaseJava

class Derived : BaseJava() {
    fun foo() = ::a.get()
}

fun box(): String {
    val d = Derived()
    return d.foo()
}
