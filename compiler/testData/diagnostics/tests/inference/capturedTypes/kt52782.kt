// FIR_IDENTICAL
// FILE: Foo.java
public abstract class Foo<K extends Bar<? extends Foo<K>>> {
    abstract String getTest();
}

// FILE: Bar.java
public abstract class Bar<T extends Foo<? extends Bar<T>>> {}

// FILE: main.kt
fun box(foo: Foo<*>) {
    foo.test // unresolved in 1.7.0, OK before
}
