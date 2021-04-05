// FILE: A.java
public @interface A {
    int a();
    double b();
    boolean x();
}

// FILE: b.kt
@A(<!ARGUMENT_TYPE_MISMATCH!>false<!>,
1.0,
false) fun foo1() {}

@A(<!ARGUMENT_TYPE_MISMATCH!>2.0<!>, x = true, b = 2.0) fun foo2() {}
