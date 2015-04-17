// FILE: A.java
public @interface A {
    Class<?>[] value();
}

// FILE: b.kt
val jClass = javaClass<String>()
A(
    <!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION, ANNOTATION_PARAMETER_MUST_BE_CLASS_LITERAL!>jClass<!>,
    <!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>javaClass<Int>()<!>
)
class MyClass1

A(
    <!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>javaClass<<!UNRESOLVED_REFERENCE!>Err<!>>()<!>,
    <!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>javaClass<String>()<!>
) class MyClass2
