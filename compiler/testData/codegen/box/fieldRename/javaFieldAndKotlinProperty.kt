// TARGET_BACKEND: JVM_IR
// DUMP_IR

// FILE: BaseJava.java
public class BaseJava {
    public String a = "OK";
}

// FILE: Derived.kt
class Derived : BaseJava() {
    val a = "FAIL"
}

fun box() = Derived().a