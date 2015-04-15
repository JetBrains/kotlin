// !DIAGNOSTICS: -JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION
// FILE: A.java
public @interface A {
    Class<?> value();
    int x() default 1;
}

// FILE: b.kt
A(javaClass<String>()) class MyClass1
A(value = javaClass<String>()) class MyClass2

A(javaClass<String>(), x = 1) class MyClass3
A(value = javaClass<String>(), x = 3) class MyClass4
