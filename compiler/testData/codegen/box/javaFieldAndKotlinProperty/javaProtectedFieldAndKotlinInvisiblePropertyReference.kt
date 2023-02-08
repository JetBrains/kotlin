// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// tried to access field base.Intermediate.a from class base.Derived$foo$1
// Field VS property: case "reference", protected field in the same package, invisible property

// FILE: BaseJava.java

package base;

public class BaseJava {
    protected String a = "OK";
}

// FILE: Derived.kt

package base
// Note: this test should report an error when we are in different package

open class Intermediate : BaseJava() {
    private val a = "FAIL"
}

class Derived : Intermediate() {
    fun foo() = this::a.get()
}

fun box(): String {
    val d = Derived()
    return d.foo()
}
