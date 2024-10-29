// FIR_IDENTICAL
// SCOPE_DUMP: KB:contains
// SCOPE_DUMP: B:contains

// FILE: I.java
public interface I {
    public boolean contains(String x) {return false;}
}

// FILE: A.java
abstract public class A implements java.util.Collection<String> {
    public boolean contains(Object x) {return false;}
}

// FILE: B.java
public abstract class B extends A implements I {}

// FILE: main.kt
abstract class KB : B()
