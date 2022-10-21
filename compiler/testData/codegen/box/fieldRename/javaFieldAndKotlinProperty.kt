// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// FIR_STATUS: accesses property backing field accidentally and fails with exception (does not work in K1/JVM too)
// Field VS property: case 4.1
// See KT-54393 for details

// FILE: BaseJava.java
public class BaseJava {
    public String a = "OK";
}

// FILE: Derived.kt
class Derived : BaseJava() {
    val a = "FAIL"
}

fun box() = Derived().a