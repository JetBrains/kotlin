// FILE: A.java
public @interface A {
    int a();
    double b();
    boolean x();
}

// FILE: b.kt
@A(1,
1.0,
false) fun foo1() {}

@A(2, x = true, b = 2.0) fun foo2() {}

@A(x = true, b = 3.0, a = 4) fun foo3() {}


