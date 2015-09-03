// FILE: A.java
public @interface A {
    String value();
    int arg();
}

// FILE: b.kt
A(value = "a", arg = 1) class MyClass

fun foo(ann: A) {
    ann.<!DEPRECATED_ANNOTATION_METHOD_CALL!>value()<!>
    ann.<!DEPRECATED_ANNOTATION_METHOD_CALL!>arg()<!>

    ann.equals(ann)
    ann.toString()
    ann.hashCode()

    MyClass::class.java.getAnnotation(A::class.java).<!DEPRECATED_ANNOTATION_METHOD_CALL!>value()<!>
    MyClass::class.java.getAnnotation(A::class.java).<!DEPRECATED_ANNOTATION_METHOD_CALL!>arg()<!>
}

fun A.bar() {
    <!DEPRECATED_ANNOTATION_METHOD_CALL!>value()<!>
    <!DEPRECATED_ANNOTATION_METHOD_CALL!>arg()<!>
    this.<!DEPRECATED_ANNOTATION_METHOD_CALL!>value()<!>
    this.<!DEPRECATED_ANNOTATION_METHOD_CALL!>arg()<!>
}
