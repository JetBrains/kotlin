// FILE: A.java
public @interface A {
    int a();
    double b();
    String value();
    boolean x();
}

// FILE: b.kt
@A("v1", <!POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>1<!>,
<!POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>1.0<!>,
<!POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>false<!>) fun foo1() {}

@A("v2", <!POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>2<!>, x = true, b = 2.0) fun foo2() {}

@A("v2", x = true, b = 3.0, a = 4) fun foo3() {}
@A(value = "v2", x = true, b = 3.0, a = 4) fun foo4() {}
