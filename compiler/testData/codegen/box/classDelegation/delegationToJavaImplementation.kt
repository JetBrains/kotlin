// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// ISSUE: KT-70683

// FILE: Base.java
public interface Base {
    String method();
}

// FILE: SimpleDerived.java
public class SimpleDerived implements Derived {
    @Override
    public String method() {
        return "OK";
    }
}

// FILE: main.kt
interface Derived : Base

class DerivedImpl(private val delegate: SimpleDerived) : Derived by delegate

fun box(): String {
    return DerivedImpl(SimpleDerived()).method()
}