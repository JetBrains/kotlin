// TARGET_BACKEND: JVM_IR
// ISSUE: KT-70683

// FILE: Base.java
public interface Base {
    String method();
    String getA();
    default void fun1() { }
    static void fun2() { }
}

// FILE: SimpleDerived.java
public class SimpleDerived implements Derived {
    @Override
    public String method() {
        return "O";
    }
    @Override
    public String getA() {
        return "K";
    }
}

// FILE: main.kt
interface Derived : Base

class DerivedImpl(private val delegate: SimpleDerived) : Derived by delegate

fun box(): String {
    return DerivedImpl(SimpleDerived()).method()+DerivedImpl(SimpleDerived()).a
}