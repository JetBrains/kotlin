// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// !LANGUAGE: -NewInference
// WITH_RUNTIME
// FILE: Base.java
import java.lang.Runnable;
import java.lang.IllegalStateException;

public class Base<T> {
    public <S> S add(Base<S> value, Runnable block) { return null; }
}

// FILE: Derived.kt
class Derived<T>(val value: T) : Base<T>() {
    init {
        add(this) {}
    }
}

fun box(): String {
    return Derived("OK").value
}
