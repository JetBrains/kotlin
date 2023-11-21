// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// FILE: Foo.java
public interface Foo {
    public static final long A = System.currentTimeMillis();
}

// FILE: test.kt
const val b = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>Foo.A<!>
