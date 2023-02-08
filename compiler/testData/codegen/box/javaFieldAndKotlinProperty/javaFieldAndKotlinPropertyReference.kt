// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// FIR status: fails because of incorrect receiver in bytecode
// Field VS property: case "reference"

// FILE: BaseJava.java
public class BaseJava {
    public String a = "OK";
}

// FILE: Derived.kt
class Derived : BaseJava() {
    private val a = "FAIL"
}

fun box(): String {
    val d = Derived()
    return d::a.get()
}