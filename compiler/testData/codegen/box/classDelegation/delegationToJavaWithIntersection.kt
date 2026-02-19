// TARGET_BACKEND: JVM_IR
// ISSUE: KT-70683

// FILE: JavaBase1.java
public interface JavaBase1 {
    void foo(Integer a);
    String bar(Integer a);
}

// FILE: JavaBase2.java
public interface JavaBase2 {
    void foo(Object a);
    Object bar(Integer a);
}

// FILE: SimpleDerived.java
public class SimpleDerived implements Derived {
    @Override
    public void foo(Integer a) {}

    @Override
    public String bar(Integer a) {
        return "OK";
    }

    @Override
    public void foo(Object a) {}
}

// FILE: main.kt
interface Derived : JavaBase1, JavaBase2

class DerivedImpl(private val delegate: SimpleDerived) : Derived by delegate

fun box(): String {
    DerivedImpl(SimpleDerived()).foo("")
    DerivedImpl(SimpleDerived()).foo(1)
    return DerivedImpl(SimpleDerived()).bar(1)
}