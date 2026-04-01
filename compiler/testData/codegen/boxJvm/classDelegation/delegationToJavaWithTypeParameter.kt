// TARGET_BACKEND: JVM_IR
// ISSUE: KT-70683

// FILE: Base.java
public interface Base<T> {
    T method();
    <K> K foo();
}

// FILE: SimpleDerived.java
public class SimpleDerived<T> implements Derived<T> {
    @Override
    public T method() {
        return (T) "O";
    }
    @Override
    public <K> K foo() {
        return (K) "K";
    }
}

// FILE: main.kt
interface Derived<T> : Base<T>

class DerivedImpl(private val delegate: SimpleDerived<String>) : Derived<String> by delegate

fun box(): String {
    return DerivedImpl(SimpleDerived()).method()+DerivedImpl(SimpleDerived()).foo<String>()
}