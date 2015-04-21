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

    javaClass<MyClass>().getAnnotation(javaClass<A>()).<!DEPRECATED_ANNOTATION_METHOD_CALL!>value()<!>
    javaClass<MyClass>().getAnnotation(javaClass<A>()).<!DEPRECATED_ANNOTATION_METHOD_CALL!>arg()<!>
}

fun A.bar() {
    <!DEPRECATED_ANNOTATION_METHOD_CALL!>value()<!>
    <!DEPRECATED_ANNOTATION_METHOD_CALL!>arg()<!>
    this.<!DEPRECATED_ANNOTATION_METHOD_CALL!>value()<!>
    this.<!DEPRECATED_ANNOTATION_METHOD_CALL!>arg()<!>
}
