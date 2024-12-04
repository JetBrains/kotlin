// TARGET_BACKEND: JVM_IR
// ISSUE: KT-70683

// FILE: Derived.java
public interface Derived extends Base { }

// FILE: SimpleDerived.java
public class SimpleDerived implements Derived {
    @Override
    public String method() {
        return "OK";
    }
}

// FILE: main.kt
interface Base {
    fun method(): String
}

class DerivedImpl(private val delegate: SimpleDerived) : Derived by delegate

fun box(): String {
    return DerivedImpl(SimpleDerived()).method()
}