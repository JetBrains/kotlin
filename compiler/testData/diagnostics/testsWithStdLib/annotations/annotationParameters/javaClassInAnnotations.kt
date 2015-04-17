// FILE: A.java
public @interface A {
    Class<?> value() default Integer.class;
    Class<?> arg() default String.class;
}

// FILE: b.kt
A(<!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>javaClass<Int>()<!>, arg = <!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>javaClass<String>()<!>) class MyClass1
A(<!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>javaClass<Int>()<!>) class MyClass2
A(arg = <!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>javaClass<String>()<!>) class MyClass3
A class MyClass4

A(value = <!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>javaClass<Int>()<!>, arg = <!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>javaClass<String>()<!>) class MyClass5
A(value = <!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>javaClass<Int>()<!>) class MyClass6
