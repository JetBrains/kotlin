// FILE: A.java
public @interface A {
    int a();
    double b();
    boolean x();
}

// FILE: b.kt
@A(false,
1.0,
false) fun foo1() {}

@A(2.0, x = true, b = 2.0) fun foo2() {}
