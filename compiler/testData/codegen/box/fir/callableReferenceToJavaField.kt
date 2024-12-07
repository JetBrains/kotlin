// TARGET_BACKEND: JVM_IR
// DUMP_IR
// FULL_JDK
// WITH_REFLECT

// FILE: bar/Base.java
package bar;

import foo.A;

public abstract class Base {
    protected A a = new A("fail");

    protected void foo() {}
}

// FILE: main.kt
package foo

import kotlin.reflect.jvm.javaField
import bar.Base

class A(val s: String)

class Derived : Base() {
    override fun foo() {
        // ir: resolved to fake-override field Derived.a in K1,
        // but to base field Base.a in K2
        // However, box() works correctly in both cases
        (Derived::a).javaField!![this] = A("OK")
    }

    fun box(): String {
        foo()
        return a.s
    }
}

fun box(): String {
    return Derived().box()
}
