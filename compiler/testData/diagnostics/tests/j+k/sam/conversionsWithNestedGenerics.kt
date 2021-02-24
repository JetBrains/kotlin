// FIR_IDENTICAL

// FILE: Listener.java
public interface Listener<T> {
    void on(T self);
}

// FILE: Base.java
public class Base<S, T extends Listener<S>> {
    public void addListener(T listener) {}
}

// FILE: Derived.java
public class Derived extends Base<Derived, Listener<Derived>> {}

// FILE: test.kt

fun test(w: Derived) {
    w.addListener { _ -> call() }
}

fun call() {}