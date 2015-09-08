// FILE: A.java
public @interface A {
    int a();
    double b();
    boolean x();
}

// FILE: b.kt
@A(<!POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>1<!>,
<!POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>1.0<!>,
<!POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>false<!>) fun foo1() {}

@A(<!POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>2<!>, x = true, b = 2.0) fun foo2() {}

@A(x = true, b = 3.0, a = 4) fun foo3() {}


