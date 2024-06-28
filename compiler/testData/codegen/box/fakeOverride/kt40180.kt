// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

// FILE: Base.java
public class Base {
    // Parameter considered as nullable by FO builder
    public void foo(String s) {}
}
// FILE: Derived.kt
class Derived : Base() {
    // Parameter is not null
    override fun foo(s: String) {}
}

fun box(): String {
    Derived().foo("")
    return "OK"
}
