// FILE: A.java
public @interface A {
    int a();
    double b();
    boolean x();
}

// FILE: b.kt
@A(false,
1.0,
false, <!TOO_MANY_ARGUMENTS!>1<!>, <!TOO_MANY_ARGUMENTS!>2<!>) fun foo1() {}
