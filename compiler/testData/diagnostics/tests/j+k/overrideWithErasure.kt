// FIR_IDENTICAL
// SKIP_TXT
// FILE: Base.java
public class Base<T extends CharSequence> {
    public T foo(T t) { return t; }
}

// FILE: Derived.java
public class Derived<E extends CharSequence> extends Base<E> {
    @Override
    public E foo(CharSequence e) {
        return (E) "";
    }
}

// FILE: main.kt

fun main(d: Derived<CharSequence>) {
    d.foo("")
}
