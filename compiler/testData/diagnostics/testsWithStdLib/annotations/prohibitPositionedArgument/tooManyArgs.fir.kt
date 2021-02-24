// FILE: A.java
public @interface A {
    int a();
    double b();
    boolean x();
}

// FILE: b.kt
<!INAPPLICABLE_CANDIDATE!>@A(false,
1.0,
false, 1, 2)<!> fun foo1() {}
