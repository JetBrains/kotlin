// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// Field VS property: case 4.1
// See KT-50082
// DUMP_IR

// FILE: BaseJava.java
public class BaseJava {
    public String a = "FAIL";
}

// FILE: Derived.kt
class Derived : BaseJava() {
    var a = "OK"
}

fun box(): String {
    val first = Derived().a
    if (first != "OK") return first
    val d = Derived()
    if (d::a.get() != "OK") return d::a.get()
    d.a = "12"
    if (d.a != "12") return "Error writing: ${d.a}"
    return "OK"
}
