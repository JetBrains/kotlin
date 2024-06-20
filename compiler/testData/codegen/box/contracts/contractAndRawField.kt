// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY
// WITH_STDLIB
// ISSUE: KT-69053

// FILE: Base.java
public abstract class Base<T> {
    protected T instance;

    protected Base(T value) {
        instance = value;
    }
}

// FILE: Derived.java
public class Derived extends Base<String> {
    public Derived(String value) {
        super(value);
    }
}

// FILE: UnsafeComplex.java
public class UnsafeComplex extends Base<Base> {
    public UnsafeComplex(Base value) {
        super(value);
    }
}

// FILE: main.kt
fun Base<Any>.confirmOrFail(): String {
    require(this is Derived)
    return instance
}

fun box(): String {
    val objA = Derived("OK")
    val complex = UnsafeComplex(objA)
    val instance: Base<Any> = complex.instance
    return instance.confirmOrFail()
}
