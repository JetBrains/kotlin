// FILE: A.java
public @interface A {
    int a();
    double b();
    boolean x();
}

// FILE: b.kt
@A(<!POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>false<!>,
<!POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>1.0<!>,
<!POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>false<!>, <!TOO_MANY_ARGUMENTS!>1<!>, <!TOO_MANY_ARGUMENTS!>2<!>) fun foo1() {}
