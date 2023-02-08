// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

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
    val first = Derived().a
    if (first != "OK") return first
    val d = Derived()
    if (d::a.get() != "OK") return d::a.get()
    d.a = "12"
    if (d.foo() != "12") return "Error writing: ${d.foo()}"
    return "OK"
}
